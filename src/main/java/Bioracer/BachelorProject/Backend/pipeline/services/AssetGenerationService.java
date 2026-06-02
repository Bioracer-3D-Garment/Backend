package Bioracer.BachelorProject.Backend.pipeline.services;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.adapters.VTONAdapter;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationJob;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationStatus;
import Bioracer.BachelorProject.Backend.pipeline.models.FailedItem;
import Bioracer.BachelorProject.Backend.pipeline.repository.AssetGenerationJobRepository;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
public class AssetGenerationService {

    private static final int maxRetries = 3;
    private static final long retryWaitMs = 2000;
    private static final List<String> positions = List.of("front", "back", "side");
    private final VTONAdapter adapter;
    private final AssetGenerationJobRepository jobRepository;
    private final CloudinaryService cloudinaryService;
    private final GeneratedAssetRepository generatedAssetRepository;
    private final ProjectRepository projectRepository;

    @Value("${pipeline.poses-dir}")
    private String posesDir;

    public AssetGenerationService(VTONAdapter adapter,
                        AssetGenerationJobRepository jobRepository,
                        CloudinaryService cloudinaryService,
                        GeneratedAssetRepository generatedAssetRepository,
                        ProjectRepository projectRepository) {
        this.adapter = adapter;
        this.jobRepository = jobRepository;
        this.cloudinaryService = cloudinaryService;
        this.generatedAssetRepository = generatedAssetRepository;
        this.projectRepository = projectRepository;
    }

    public AssetGenerationJob createJob(int totalCount, Long folderId) {
        if (!projectRepository.existsById(folderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Project not found: " + folderId);
        }
        String jobId = UUID.randomUUID().toString();
        String runId = "run_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return jobRepository.save(new AssetGenerationJob(jobId, runId, null, totalCount, folderId));
    }

    public AssetGenerationJob submitAssetGeneration(MultipartFile frontDesign,
                                                    MultipartFile backDesign,
                                                    Long modelId,
                                                    Long folderId) throws IOException {

        String productId = resolveProductId(frontDesign.getOriginalFilename());
        byte[] frontDesignBytes = frontDesign.getBytes();
        byte[] backDesignBytes = backDesign.getBytes();

        AssetGenerationJob job = createJob(positions.size(), folderId);
        runAssetGeneration(job.getJobId(), productId, frontDesignBytes, backDesignBytes, modelId);
        return job;
    }

    /**
     * Returns the single pose image for the given modelId and pose.
     * Throws IllegalStateException if the directory is missing or contains no images.
     */
    public Path resolvePoseFileForpose(Long modelId, String pose) {
        Path dir = Paths.get(posesDir, modelId.toString(), pose);
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException(
                    "No pose directory for modelId='" + modelId + "' Position='" + pose + "': " + dir);
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> isImageFile(p.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No pose image found in " + dir));
        } catch (IOException e) {
            throw new IllegalStateException("Error reading pose directory: " + dir, e);
        }
    }

    @Async
    public void runAssetGeneration(String jobId,
                                   String productId,
                                   byte[] frontDesignBytes,
                                   byte[] backDesignBytes,
                                   Long modelId) {
        AssetGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jobId: " + jobId));

        try {
            job.setStatus(AssetGenerationStatus.RUNNING);
            jobRepository.save(job);

            List<FailedItem> failedItems = processAllCombinations(job, productId, frontDesignBytes, backDesignBytes, modelId);

            job.setFailedItems(new ArrayList<>(failedItems));
            if (failedItems.isEmpty()) {
                job.setStatus(AssetGenerationStatus.DONE);
            } else if (job.getUploadedCount() > 0) {
                job.setStatus(AssetGenerationStatus.PARTIAL);
            } else {
                job.setStatus(AssetGenerationStatus.FAILED);
            }
            jobRepository.save(job);

        } catch (Exception e) {
            fail(job, e.getMessage());
            throw new IllegalStateException("Asset generation job failed: " + jobId, e);
        }
    }

    // ---- private helpers ----

    private List<FailedItem> processAllCombinations(AssetGenerationJob job,
                                                     String productId,
                                                     byte[] frontDesignBytes,
                                                     byte[] backDesignBytes,
                                                     Long modelId) {
        List<FailedItem> failedItems = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        // Virtual threads: one per garment×pose combination for maximum I/O concurrency
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String pose : positions) {
                futures.add(executor.submit(() -> {
                    Optional<String> failure = processOneWithRetry(productId, frontDesignBytes, backDesignBytes, pose, modelId, job);
                    failure.ifPresent(reason ->
                            failedItems.add(new FailedItem(productId, pose, reason)));
                    recordCompleted(job);
                }));
            }

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception e) {
                    throw new IllegalStateException("Unexpected executor error", e);
                }
            }
        }

        return failedItems;
    }

    /** Returns empty on success, or the failure reason string. */
    private Optional<String> processOneWithRetry(String productId,
                                                  byte[] frontDesignBytes,
                                                  byte[] backDesignBytes,
                                                  String pose,
                                                  Long modelId,
                                                  AssetGenerationJob job) {
        Path poseFile;
        try {
            poseFile = resolvePoseFileForpose(modelId, pose);
        } catch (IllegalStateException e) {
            return Optional.of("Pose directory missing: " + e.getMessage());
        }

        String cloudinaryPublicId = "bioracer/" + job.getFolderId()
                + "/" + productId + "/" + pose;
        String lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] poseBytes = Files.readAllBytes(poseFile);
                byte[] result = adapter.generate(frontDesignBytes, backDesignBytes, poseBytes, null, null);

                CloudinaryService.UploadResult uploadResult =
                        cloudinaryService.upload(result, cloudinaryPublicId);

                GeneratedAsset asset = new GeneratedAsset(
                        projectRepository.getReferenceById(job.getFolderId()),
                        job.getJobId(),
                        productId,
                        pose,
                        null,
                        uploadResult.secureUrl(),
                        uploadResult.thumbnailUrl(),
                        uploadResult.publicId());
                generatedAssetRepository.save(asset);
                recordUploaded(job);

                return Optional.empty();

            } catch (Exception e) {
                lastError = e.getMessage();

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryWaitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Optional.of("interrupted");
                    }
                }
            }
        }

        return Optional.of(lastError != null ? lastError : "max retries exceeded");
    }

    private String resolveProductId(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "front-design";
        }

        String fileName = originalFilename.replace('\\', '/');
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }

        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    public static boolean isImageFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    // completed increments on both success and failure so the progress bar reaches 100 %
    private synchronized void recordCompleted(AssetGenerationJob job) {
        job.setCompletedCount(job.getCompletedCount() + 1);
        jobRepository.save(job);
    }

    private synchronized void recordUploaded(AssetGenerationJob job) {
        job.setUploadedCount(job.getUploadedCount() + 1);
        jobRepository.save(job);
    }

    private void fail(AssetGenerationJob job, String message) {
        job.setStatus(AssetGenerationStatus.FAILED);
        job.setErrorMessage(message);
        jobRepository.save(job);
    }

}
