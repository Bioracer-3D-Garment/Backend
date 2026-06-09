package Bioracer.BachelorProject.Backend.pipeline.services;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.model.Model;
import Bioracer.BachelorProject.Backend.pipeline.adapters.VTONAdapter;
import Bioracer.BachelorProject.Backend.pipeline.models.AdvancedSettings;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationJob;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationStatus;
import Bioracer.BachelorProject.Backend.pipeline.models.FailedItem;
import Bioracer.BachelorProject.Backend.pipeline.repository.AssetGenerationJobRepository;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import Bioracer.BachelorProject.Backend.repository.ModelRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Java 21 virtual threads required: Executors.newVirtualThreadPerTaskExecutor() is used for
// parallel combination processing. Downgrading to an earlier JVM will break this class.
@Service
public class AssetGenerationService {

    private static final int maxRetries = 3;
    private static final long retryWaitMs = 2000;
    private static final List<String> positions = List.of("front", "back", "side");
    private final VTONAdapter adapter;
    private final AssetGenerationJobRepository jobRepository;
    private final UploadService cloudinaryService;
    private final GeneratedAssetRepository generatedAssetRepository;
    private final ProjectRepository projectRepository;
    private final ModelRepository modelRepository;

    public AssetGenerationService(VTONAdapter adapter,
            AssetGenerationJobRepository jobRepository,
<<<<<<< Updated upstream
            CloudinaryService cloudinaryService,
=======
            UploadService cloudinaryService,
>>>>>>> Stashed changes
            GeneratedAssetRepository generatedAssetRepository,
            ProjectRepository projectRepository,
            ModelRepository modelRepository) {
        this.adapter = adapter;
        this.jobRepository = jobRepository;
        this.cloudinaryService = cloudinaryService;
        this.generatedAssetRepository = generatedAssetRepository;
        this.projectRepository = projectRepository;
        this.modelRepository = modelRepository;
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
<<<<<<< Updated upstream
            Long modelId,
            Long folderId,
            AdvancedSettings advancedSettings) throws IOException {
=======
            MultipartFile backDesign,
            Long modelId,
            Long folderId) throws IOException {
>>>>>>> Stashed changes

        // Resolve the model's pose images (Cloudinary public IDs) up front so a bad
        // model
        // fails fast before the async job is created.
        Map<String, String> poseImageIds = resolvePoseImageIds(modelId);

        String productId = resolveProductId(frontDesign.getOriginalFilename());
        byte[] frontDesignBytes = frontDesign.getBytes();

        AssetGenerationJob job = createJob(positions.size(), folderId);
        runAssetGeneration(job.getJobId(), productId, frontDesignBytes, poseImageIds, advancedSettings);
        return job;
    }

    /**
<<<<<<< Updated upstream
     * Returns the Cloudinary public ID of each pose image (front/back/side) for the
     * given model.
=======
     * Returns the backend filename or file URL of each pose image (front/back/side)
     * for the given model.
>>>>>>> Stashed changes
     * Throws 404 if the model does not exist.
     */
    private Map<String, String> resolvePoseImageIds(Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Model not found: " + modelId));

        Map<String, String> poseImageIds = new LinkedHashMap<>();
        poseImageIds.put("front", model.getFront());
        poseImageIds.put("back", model.getBack());
        poseImageIds.put("side", model.getSide());
        return poseImageIds;
    }

    @Async
    public void runAssetGeneration(String jobId,
            String productId,
            byte[] frontDesignBytes,
<<<<<<< Updated upstream
            Map<String, String> poseImageIds,
            AdvancedSettings advancedSettings) {
=======
            byte[] backDesignBytes,
            Map<String, String> poseImageIds) {
>>>>>>> Stashed changes
        AssetGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jobId: " + jobId));

        try {
            job.setStatus(AssetGenerationStatus.RUNNING);
            jobRepository.save(job);

<<<<<<< Updated upstream
            List<FailedItem> failedItems = processAllCombinations(job, productId, frontDesignBytes, poseImageIds,
                    advancedSettings);
=======
            List<FailedItem> failedItems = processAllCombinations(job, productId, frontDesignBytes, backDesignBytes,
                    poseImageIds);
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
            Map<String, String> poseImageIds,
            AdvancedSettings advancedSettings) {
=======
            byte[] backDesignBytes,
            Map<String, String> poseImageIds) {
>>>>>>> Stashed changes
        List<FailedItem> failedItems = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        // Virtual threads: one per garment×pose combination for maximum I/O concurrency
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String pose : positions) {
                String posePublicId = poseImageIds.get(pose);
                futures.add(executor.submit(() -> {
<<<<<<< Updated upstream
                    Optional<String> failure = processOneWithRetry(productId, frontDesignBytes, pose, posePublicId, job,
                            advancedSettings);
=======
                    Optional<String> failure = processOneWithRetry(productId, frontDesignBytes, backDesignBytes, pose,
                            posePublicId, job);
>>>>>>> Stashed changes
                    failure.ifPresent(reason -> failedItems.add(new FailedItem(productId, pose, reason)));
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
<<<<<<< Updated upstream
            String pose,
            String posePublicId,
            AssetGenerationJob job,
            AdvancedSettings advancedSettings) {
=======
            byte[] backDesignBytes,
            String pose,
            String posePublicId,
            AssetGenerationJob job) {
>>>>>>> Stashed changes
        if (posePublicId == null || posePublicId.isBlank()) {
            return Optional.of("Model has no '" + pose + "' pose image");
        }

        byte[] poseBytes;
        try {
            poseBytes = cloudinaryService.download(posePublicId);
        } catch (Exception e) {
            return Optional.of("Failed to download pose '" + pose + "' (" + posePublicId + "): " + e.getMessage());
        }

<<<<<<< Updated upstream
        String cloudinaryPublicId = job.getJobId() + "_" + job.getFolderId() + "_" + productId + "_" + pose;
=======
        String generatedFilename = productId + "_" + pose + ".jpg";
>>>>>>> Stashed changes
        String lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] result = adapter.generate(frontDesignBytes, poseBytes, lastError, advancedSettings);

<<<<<<< Updated upstream
                CloudinaryService.UploadResult uploadResult = cloudinaryService.upload(result, cloudinaryPublicId);
=======
                UploadService.UploadResult uploadResult = cloudinaryService.upload(result, generatedFilename);
>>>>>>> Stashed changes

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

    // completed increments on both success and failure so the progress bar reaches
    // 100 %
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
