package Bioracer.BachelorProject.Backend.pipeline.services;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.adapters.KlingAdapter;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationJob;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationStatus;
import Bioracer.BachelorProject.Backend.pipeline.repository.AssetGenerationJobRepository;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.service.UploadService;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoGenerationService {

    private static final String VIDEO_POSE = "video";
    private static final String VIDEO_CATEGORY = "video";

    private final KlingAdapter videoClient;
    private final AssetGenerationJobRepository jobRepository;
    private final GeneratedAssetRepository generatedAssetRepository;
    private final ProjectRepository projectRepository;
    private final UploadService uploadService;

    public VideoGenerationService(
            KlingAdapter videoClient,
            AssetGenerationJobRepository jobRepository,
            GeneratedAssetRepository generatedAssetRepository,
            ProjectRepository projectRepository,
            UploadService uploadService) {

        this.videoClient = videoClient;
        this.jobRepository = jobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.projectRepository = projectRepository;
        this.uploadService = uploadService;
    }

    public AssetGenerationJob submitVideoGeneration(
            String imageJobId,
            String productId,
            Long folderId,
            Integer durationSeconds,
            String prompt) {

        if (!projectRepository.existsById(folderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Project not found: " + folderId);
        }

        Map<String, String> poseUrls = resolvePoseUrls(imageJobId, productId);

        String frontUrl = toPublicUrl(poseUrls.get("front"));
        String backUrl = toPublicUrl(poseUrls.get("back"));

        if (frontUrl == null || backUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing required front/back images for video generation");
        }

        List<String> references = new ArrayList<>();
        String sideUrl = poseUrls.get("side");
        if (sideUrl != null) {
            references.add(toPublicUrl(sideUrl));
        }

        AssetGenerationJob job = createJob(folderId);

        runVideoGeneration(
                job.getJobId(),
                productId,
                folderId,
                frontUrl,
                backUrl,
                references,
                durationSeconds,
                prompt);

        return job;
    }

    private AssetGenerationJob createJob(Long folderId) {
        String jobId = UUID.randomUUID().toString();
        String runId = "video_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return jobRepository.save(
                new AssetGenerationJob(jobId, runId, null, 1, folderId));
    }

    @Async
    public void runVideoGeneration(
            String jobId,
            String productId,
            Long folderId,
            String frontUrl,
            String backUrl,
            List<String> referenceUrls,
            Integer durationSeconds,
            String prompt) {

        AssetGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jobId: " + jobId));

        try {
            job.setStatus(AssetGenerationStatus.RUNNING);
            jobRepository.save(job);

            byte[] video = videoClient.generate(
                    frontUrl,
                    backUrl,
                    referenceUrls,
                    durationSeconds,
                    prompt);

            String publicId = job.getJobId()
                    + "_" + job.getFolderId()
                    + "_" + productId
                    + "_video";

            UploadService.UploadResult uploadResult = uploadService.uploadVideo(video, publicId);

            GeneratedAsset asset = new GeneratedAsset(
                    projectRepository.getReferenceById(folderId),
                    jobId,
                    productId,
                    VIDEO_POSE,
                    VIDEO_CATEGORY,
                    uploadResult.secureUrl(),
                    frontUrl,
                    uploadResult.publicId());

            generatedAssetRepository.save(asset);

            job.setCompletedCount(1);
            job.setUploadedCount(1);
            job.setStatus(AssetGenerationStatus.DONE);
            jobRepository.save(job);

        } catch (Exception e) {
            job.setCompletedCount(1);
            job.setStatus(AssetGenerationStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        }
    }

    private String toPublicUrl(String url) {
        if (url == null)
            return null;

        if (url.startsWith("http://localhost")) {
            String publicBase = System.getenv("PUBLIC_URL_BASE");

            if (publicBase == null || publicBase.isBlank()) {
                throw new IllegalStateException(
                        "PUBLIC_URL_BASE must be set");
            }

            return url.replace("http://localhost:8081", publicBase);
        }

        return url;
    }

    private Map<String, String> resolvePoseUrls(String imageJobId, String productId) {
        return generatedAssetRepository.findByJobId(imageJobId).stream()
                .filter(a -> productId.equals(a.getProductId()))
                .filter(a -> a.getPoseId() != null && a.getSecureUrl() != null)
                .collect(Collectors.toMap(
                        GeneratedAsset::getPoseId,
                        GeneratedAsset::getSecureUrl,
                        (a, b) -> a));
    }
}