package Bioracer.BachelorProject.Backend.pipeline.services;

import Bioracer.BachelorProject.Backend.pipeline.adapters.VTONAdapter;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJobRepository;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchStatus;
import Bioracer.BachelorProject.Backend.pipeline.models.FailedItem;
import Bioracer.BachelorProject.Backend.pipeline.utils.CatalogProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

// Java 21 virtual threads required: Executors.newVirtualThreadPerTaskExecutor() is used for
// parallel combination processing. Downgrading to an earlier JVM will break this class.
@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    private static final int MAX_RETRIES    = 3;
    private static final long RETRY_WAIT_MS = 2_000;

    private static final DateTimeFormatter RUN_ID_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final VTONAdapter adapter;
    private final BatchJobRepository jobRepository;

    @Value("${pipeline.poses-dir}")
    private String posesDir;

    @Value("${pipeline.output-dir}")
    private String outputDir;

    public BatchService(VTONAdapter adapter, BatchJobRepository jobRepository) {
        this.adapter = adapter;
        this.jobRepository = jobRepository;
    }

    public BatchJob createJob(int totalCount, Long folderId) {
        String jobId   = UUID.randomUUID().toString();
        String runId   = "run_" + LocalDateTime.now().format(RUN_ID_FMT);
        String outPath = Paths.get(outputDir, jobId).toAbsolutePath().toString();
        return jobRepository.save(new BatchJob(jobId, runId, outPath, totalCount, folderId));
    }

    /**
     * Returns all image files in {poses-dir}/{gender}/{top|bottom}/.
     * garmentCategory is the Fashn.ai value: "upper_body" or "lower_body".
     * An empty list means the directory does not exist or contains no images.
     */
    public List<Path> resolvePoseFiles(String gender, String garmentCategory) {
        Path dir = Paths.get(posesDir, gender, categoryToFolder(garmentCategory));
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> isImageFile(p.getFileName().toString())).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Maps Fashn.ai category values to the subfolder name used on disk. */
    public static String categoryToFolder(String garmentCategory) {
        return switch (garmentCategory) {
            case "upper_body" -> "top";
            case "lower_body" -> "bottom";
            default           -> garmentCategory;
        };
    }

    @Async
    public void runBatch(String jobId, List<CatalogProduct> products, String gender) {
        BatchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jobId: " + jobId));

        try {
            Path jobOutputDir = Paths.get(job.getOutputPath());
            Files.createDirectories(jobOutputDir);

            job.setStatus(BatchStatus.RUNNING);
            jobRepository.save(job);

            log.info("Batch job {} started: {} combinations", jobId, job.getTotalCount());

            List<FailedItem> failedItems = processAllCombinations(job, products, gender, jobOutputDir);

            job.setFailedItems(new ArrayList<>(failedItems));
            job.setStatus(failedItems.isEmpty() ? BatchStatus.DONE : BatchStatus.FAILED);
            jobRepository.save(job);
            log.info("Batch job {} finished — status={} failed={}",
                    jobId, job.getStatus(), failedItems.size());

        } catch (Exception e) {
            log.error("Batch job {} encountered a fatal error", jobId, e);
            fail(job, e.getMessage());
        }
    }

    // ---- private helpers ----

    private List<FailedItem> processAllCombinations(BatchJob job,
                                                     List<CatalogProduct> products,
                                                     String gender,
                                                     Path jobOutputDir) {
        List<FailedItem> failedItems = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        // Virtual threads: one per product×pose combination for maximum I/O concurrency
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (CatalogProduct product : products) {
                // Each product uses the pose set matching its garment category:
                // upper_body → {gender}/top/, lower_body → {gender}/bottom/
                List<Path> poseFiles = resolvePoseFiles(gender, product.category());
                for (Path poseFile : poseFiles) {
                    String poseId = stripExtension(poseFile.getFileName().toString());
                    futures.add(executor.submit(() -> {
                        Optional<String> failure = processOneWithRetry(product, poseId, poseFile, jobOutputDir);
                        failure.ifPresent(reason ->
                                failedItems.add(new FailedItem(product.productId(), poseId, reason)));
                        recordCompleted(job);
                    }));
                }
            }

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception e) {
                    log.error("Unexpected executor error", e);
                }
            }
        }

        return failedItems;
    }

    /** Returns empty on success, or the failure reason string. */
    private Optional<String> processOneWithRetry(CatalogProduct product,
                                                  String poseId,
                                                  Path poseFile,
                                                  Path outputDir) {
        Path outputFile = outputDir.resolve(product.productId() + "__" + poseId + ".png");
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                byte[] result = adapter.generate(
                        product.garmentImageBytes(),
                        Files.readAllBytes(poseFile),
                        product.category(),
                        null);

                Files.write(outputFile, result);
                log.info("Generated {}", outputFile.getFileName());
                return Optional.empty();

            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Attempt {}/{} failed for product={} pose={}: {}",
                        attempt, MAX_RETRIES, product.productId(), poseId, lastError);

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_WAIT_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Optional.of("interrupted");
                    }
                }
            }
        }

        log.error("All {} retries exhausted for product={} pose={}", MAX_RETRIES,
                product.productId(), poseId);
        return Optional.of(lastError != null ? lastError : "max retries exceeded");
    }

    // completed increments on both success and failure so the progress bar reaches 100 %
    private synchronized void recordCompleted(BatchJob job) {
        job.setCompletedCount(job.getCompletedCount() + 1);
        jobRepository.save(job);
    }

    private void fail(BatchJob job, String message) {
        job.setStatus(BatchStatus.FAILED);
        job.setErrorMessage(message);
        jobRepository.save(job);
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }

    public static boolean isImageFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp");
    }
}
