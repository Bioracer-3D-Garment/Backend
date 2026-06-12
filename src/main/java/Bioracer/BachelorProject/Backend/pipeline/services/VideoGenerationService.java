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
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
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
        Map<String, String> poseId = resolvePoseId(imageJobId, productId);

        String frontUrl = poseUrls.get("front");
        String backUrl = poseUrls.get("back");

        String frontId = poseId.get("front");

        if (frontUrl == null || backUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing required front/back images for video generation");
        }

        byte[] frontBytes = fetchImageBytes(frontUrl);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("FRONT BYTES: " + frontBytes.length);
        System.out.println(frontBytes);
        byte[] backBytes = fetchImageBytes(backUrl);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        AssetGenerationJob job = createJob(folderId);

        runVideoGeneration(
                job.getJobId(),
                productId,
                folderId,
                frontId,
                frontBytes,
                backBytes,
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
            String frontId,
            byte[] frontBytes,
            byte[] backBytes,
            Integer durationSeconds,
            String prompt) {

        AssetGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jobId: " + jobId));

        try {
            job.setStatus(AssetGenerationStatus.RUNNING);
            jobRepository.save(job);

            byte[] video = videoClient.generate(
                    frontBytes,
                    backBytes,
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
                    frontId,
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

    private byte[] fetchImageBytes(String url) {
        try {
            return RestClient.create().get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to fetch image bytes from: " + url, e);
        }
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

    private Map<String, String> resolvePoseId(String imageJobId, String productId) {
        return generatedAssetRepository.findByJobId(imageJobId).stream()
                .filter(a -> productId.equals(a.getProductId()))
                .filter(a -> a.getPoseId() != null && a.getSecureUrl() != null)
                .collect(Collectors.toMap(
                        GeneratedAsset::getPoseId,
                        GeneratedAsset::getPublicId,
                        (a, b) -> a));
    }
}