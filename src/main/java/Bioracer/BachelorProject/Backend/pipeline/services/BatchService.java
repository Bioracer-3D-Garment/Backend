package Bioracer.BachelorProject.Backend.pipeline.services;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.adapters.VTONAdapter;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchStatus;
import Bioracer.BachelorProject.Backend.pipeline.models.FailedItem;
import Bioracer.BachelorProject.Backend.pipeline.repository.BatchJobRepository;
import Bioracer.BachelorProject.Backend.pipeline.utils.GarmentZipEntry;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;

// Java 21 virtual threads required: Executors.newVirtualThreadPerTaskExecutor() is used for
// parallel combination processing. Downgrading to an earlier JVM will break this class.
@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    private static final int MAX_RETRIES    = 3;
    private static final long RETRY_WAIT_MS = 2_000;

    private static final List<String> ANGLES = List.of("front", "back", "side");

    private static final DateTimeFormatter RUN_ID_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final VTONAdapter adapter;
    private final BatchJobRepository jobRepository;
    private final CloudinaryService cloudinaryService;
    private final GeneratedAssetRepository generatedAssetRepository;
    private final ProjectRepository projectRepository;

    @Value("${pipeline.poses-dir}")
    private String posesDir;

    public BatchService(VTONAdapter adapter,
                        BatchJobRepository jobRepository,
                        CloudinaryService cloudinaryService,
                        GeneratedAssetRepository generatedAssetRepository,
                        ProjectRepository projectRepository) {
        this.adapter = adapter;
        this.jobRepository = jobRepository;
        this.cloudinaryService = cloudinaryService;
        this.generatedAssetRepository = generatedAssetRepository;
        this.projectRepository = projectRepository;
    }

    public BatchJob createJob(int totalCount, Long folderId) {
        if (!projectRepository.existsById(folderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Project not found: " + folderId);
        }
        String jobId = UUID.randomUUID().toString();
        String runId = "run_" + LocalDateTime.now().format(RUN_ID_FMT);
        return jobRepository.save(new BatchJob(jobId, runId, null, totalCount, folderId));
    }

    public BatchJob submitBatch(MultipartFile garmentZip, String gender, Long folderId) throws IOException {
        String normalizedGender = normalizeGender(gender);
        validatePoseDirectories(normalizedGender);

        List<GarmentZipEntry> garments = parseGarmentZip(garmentZip);
        int total = garments.size() * ANGLES.size();

        BatchJob job = createJob(total, folderId);
        runBatch(job.getJobId(), garments, normalizedGender);
        return job;
    }

    /**
     * Returns the single pose image for the given gender and angle.
     * Throws IllegalStateException if the directory is missing or contains no images.
     */
    public Path resolvePoseFileForAngle(String gender, String angle) {
        Path dir = Paths.get(posesDir, gender, angle);
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException(
                    "No pose directory for gender='" + gender + "' angle='" + angle + "': " + dir);
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

    private void validatePoseDirectories(String gender) {
        for (String angle : ANGLES) {
            try {
                resolvePoseFileForAngle(gender, angle);
            } catch (IllegalStateException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gender is required");
        }
        return gender.trim().toLowerCase();
    }

    private List<GarmentZipEntry> parseGarmentZip(MultipartFile garmentZip) throws IOException {
        Map<String, Map<String, byte[]>> garmentImages = new LinkedHashMap<>();
        Map<String, String> garmentCategories = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(garmentZip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String normalizedPath = entry.getName().replace('\\', '/');
                String[] parts = normalizedPath.split("/");
                if (parts.length < 2) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid garment ZIP entry: " + entry.getName());
                }

                String garmentName = parts[0];
                String fileName = parts[parts.length - 1].toLowerCase();
                byte[] bytes = zipInputStream.readAllBytes();

                garmentImages.computeIfAbsent(garmentName, key -> new LinkedHashMap<>());

                if ("category.txt".equals(fileName)) {
                    garmentCategories.put(garmentName, new String(bytes, StandardCharsets.UTF_8).trim().toLowerCase());
                } else if (isImageFile(fileName)) {
                    String angle = fileName.substring(0, fileName.lastIndexOf('.'));
                    if (!ANGLES.contains(angle)) {
                        continue;
                    }
                    garmentImages.get(garmentName).put(angle, bytes);
                }
            }
        }

        List<GarmentZipEntry> garments = new ArrayList<>();
        for (Map.Entry<String, Map<String, byte[]>> garmentEntry : garmentImages.entrySet()) {
            String garmentName = garmentEntry.getKey();
            Map<String, byte[]> angleImages = garmentEntry.getValue();
            String category = garmentCategories.get(garmentName);

            if (category == null || category.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Missing category.txt for garment: " + garmentName);
            }

            for (String angle : ANGLES) {
                if (!angleImages.containsKey(angle)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Missing " + angle + ".jpg for garment: " + garmentName);
                }
            }

            garments.add(new GarmentZipEntry(garmentName, category, angleImages));
        }

        if (garments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No garments found in uploaded ZIP");
        }

        return garments;
    }

    @Async
    public void runBatch(String jobId, List<GarmentZipEntry> garments, String gender) {
        BatchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jobId: " + jobId));

        try {
            job.setStatus(BatchStatus.RUNNING);
            jobRepository.save(job);

            log.info("Batch job {} started: {} combinations", jobId, job.getTotalCount());

            List<FailedItem> failedItems = processAllCombinations(job, garments, gender);

            job.setFailedItems(new ArrayList<>(failedItems));
            if (failedItems.isEmpty()) {
                job.setStatus(BatchStatus.DONE);
            } else if (job.getUploadedCount() > 0) {
                job.setStatus(BatchStatus.PARTIAL);
            } else {
                job.setStatus(BatchStatus.FAILED);
            }
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
                                                     List<GarmentZipEntry> garments,
                                                     String gender) {
        List<FailedItem> failedItems = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        // Virtual threads: one per garment×angle combination for maximum I/O concurrency
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (GarmentZipEntry garment : garments) {
                for (String angle : ANGLES) {
                    futures.add(executor.submit(() -> {
                        Optional<String> failure = processOneWithRetry(garment, angle, gender, job);
                        failure.ifPresent(reason ->
                                failedItems.add(new FailedItem(garment.garmentName(), angle, reason)));
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
    private Optional<String> processOneWithRetry(GarmentZipEntry garment,
                                                  String angle,
                                                  String gender,
                                                  BatchJob job) {
        Path poseFile;
        try {
            poseFile = resolvePoseFileForAngle(gender, angle);
        } catch (IllegalStateException e) {
            return Optional.of("Pose directory missing: " + e.getMessage());
        }

        byte[] garmentBytes = garment.angleImages().get(angle);
        // Cloudinary path: bioracer/{projectId}/{garmentName}/{pose}
        String cloudinaryPublicId = "bioracer/" + job.getFolderId()
                + "/" + garment.garmentName() + "/" + angle;
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                byte[] poseBytes = Files.readAllBytes(poseFile);
                byte[] result = adapter.generate(garmentBytes, poseBytes, garment.category(), null);

                CloudinaryService.UploadResult uploadResult =
                        cloudinaryService.upload(result, cloudinaryPublicId);

                // getReferenceById avoids loading the entity and is safe across session boundaries
                GeneratedAsset asset = new GeneratedAsset(
                        projectRepository.getReferenceById(job.getFolderId()),
                        job.getJobId(),
                        garment.garmentName(),
                        angle,
                        garment.category(),
                        uploadResult.secureUrl(),
                        uploadResult.thumbnailUrl(),
                        uploadResult.publicId());
                generatedAssetRepository.save(asset);
                recordUploaded(job);

                log.info("Uploaded asset for garment={} angle={}", garment.garmentName(), angle);
                return Optional.empty();

            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Attempt {}/{} failed for garment={} angle={}: {}",
                        attempt, MAX_RETRIES, garment.garmentName(), angle, lastError);

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

        log.error("All {} retries exhausted for garment={} angle={}", MAX_RETRIES,
                garment.garmentName(), angle);
        return Optional.of(lastError != null ? lastError : "max retries exceeded");
    }

    // completed increments on both success and failure so the progress bar reaches 100 %
    private synchronized void recordCompleted(BatchJob job) {
        job.setCompletedCount(job.getCompletedCount() + 1);
        jobRepository.save(job);
    }

    private synchronized void recordUploaded(BatchJob job) {
        job.setUploadedCount(job.getUploadedCount() + 1);
        jobRepository.save(job);
    }

    private void fail(BatchJob job, String message) {
        job.setStatus(BatchStatus.FAILED);
        job.setErrorMessage(message);
        jobRepository.save(job);
    }

    public static boolean isImageFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp");
    }
}
