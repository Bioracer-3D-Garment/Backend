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
import java.util.*;
import java.util.concurrent.*;

@Service
public class AssetGenerationService {

    private static final int maxRetries = 3;
    private static final long retryWaitMs = 2000;
    private static final List<String> positions = List.of("front", "back", "side");

    private final VTONAdapter adapter;
    private final AssetGenerationJobRepository jobRepository;
    private final UploadService uploadService;
    private final GeneratedAssetRepository generatedAssetRepository;
    private final ProjectRepository projectRepository;
    private final ModelRepository modelRepository;

    public AssetGenerationService(
            VTONAdapter adapter,
            AssetGenerationJobRepository jobRepository,
            UploadService uploadService,
            GeneratedAssetRepository generatedAssetRepository,
            ProjectRepository projectRepository,
            ModelRepository modelRepository) {

        this.adapter = adapter;
        this.jobRepository = jobRepository;
        this.uploadService = uploadService;
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
        String runId = "run_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return jobRepository.save(
                new AssetGenerationJob(jobId, runId, null, totalCount, folderId));
    }

    public AssetGenerationJob submitAssetGeneration(
            MultipartFile frontDesign,
            MultipartFile backDesign,
            Long modelId,
            Long folderId,
            AdvancedSettings advancedSettings) throws IOException {

        Map<String, String> poseImageIds = resolvePoseImageIds(modelId);

        String productId = resolveProductId(frontDesign.getOriginalFilename());
        String extension = getExtension(frontDesign.getOriginalFilename());

        byte[] frontDesignBytes = frontDesign.getBytes();
        byte[] backDesignBytes = backDesign.getBytes();

        AssetGenerationJob job = createJob(positions.size(), folderId);

        runAssetGeneration(
                job.getJobId(),
                productId,
                extension,
                frontDesignBytes,
                backDesignBytes,
                poseImageIds,
                advancedSettings);

        return job;
    }

    private Map<String, String> resolvePoseImageIds(Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Model not found: " + modelId));

        Map<String, String> map = new LinkedHashMap<>();
        map.put("front", model.getFront());
        map.put("back", model.getBack());
        map.put("side", model.getSide());
        return map;
    }

    @Async
    public void runAssetGeneration(
            String jobId,
            String productId,
            String extension,
            byte[] frontDesignBytes,
            byte[] backDesignBytes,
            Map<String, String> poseImageIds,
            AdvancedSettings advancedSettings) {

        AssetGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jobId: " + jobId));

        try {
            job.setStatus(AssetGenerationStatus.RUNNING);
            jobRepository.save(job);

            List<FailedItem> failedItems = processAllCombinations(
                    job,
                    productId,
                    extension,
                    frontDesignBytes,
                    backDesignBytes,
                    poseImageIds,
                    advancedSettings);

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
            throw new IllegalStateException("Asset generation failed: " + jobId, e);
        }
    }

    private List<FailedItem> processAllCombinations(
            AssetGenerationJob job,
            String productId,
            String extension,
            byte[] frontDesignBytes,
            byte[] backDesignBytes,
            Map<String, String> poseImageIds,
            AdvancedSettings advancedSettings) {

        List<FailedItem> failedItems = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<?>> futures = new ArrayList<>();

            for (String pose : positions) {
                String posePublicId = poseImageIds.get(pose);

                futures.add(executor.submit(() -> {
                    Optional<String> failure = processOneWithRetry(
                            job,
                            productId,
                            extension,
                            frontDesignBytes,
                            backDesignBytes,
                            pose,
                            posePublicId,
                            advancedSettings);

                    failure.ifPresent(reason -> failedItems.add(new FailedItem(productId, pose, reason)));

                    recordCompleted(job);
                }));
            }

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception e) {
                    throw new IllegalStateException("Executor failure", e);
                }
            }
        }

        return failedItems;
    }

    private Optional<String> processOneWithRetry(
            AssetGenerationJob job,
            String productId,
            String extension,
            byte[] frontDesignBytes,
            byte[] backDesignBytes,
            String pose,
            String posePublicId,
            AdvancedSettings advancedSettings) {

        if (posePublicId == null || posePublicId.isBlank()) {
            return Optional.of("Missing pose image: " + pose);
        }

        byte[] poseBytes;
        try {
            poseBytes = uploadService.download(posePublicId);
        } catch (Exception e) {
            return Optional.of("Download failed: " + e.getMessage());
        }

        String filenameBase = job.getJobId()
                + "_" + job.getFolderId()
                + "_" + productId
                + "_" + pose;

        String filename = filenameBase + extension;

        String lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] result = adapter.generate(
                        frontDesignBytes,
                        backDesignBytes,
                        poseBytes,
                        lastError,
                        advancedSettings);

                UploadService.UploadResult uploadResult = uploadService.upload(result, filename);

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

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank())
            return ".png";

        int dot = filename.lastIndexOf('.');
        if (dot == -1)
            return ".png";

        return filename.substring(dot);
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