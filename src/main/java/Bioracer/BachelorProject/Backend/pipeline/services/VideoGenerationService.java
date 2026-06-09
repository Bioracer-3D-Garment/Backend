package Bioracer.BachelorProject.Backend.pipeline.services;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.adapters.KlingAdapter;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationJob;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationStatus;
import Bioracer.BachelorProject.Backend.pipeline.repository.AssetGenerationJobRepository;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.service.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates a "turntable" video for a product on top of the already-generated Fashn try-on images.
 *
 * <p>The front image is the video's start frame, the back image is the end frame, and the side
 * image (when present) is passed as an extra reference for detail. Runs asynchronously and tracks
 * progress through the shared {@link AssetGenerationJob} machinery, so the frontend can poll the
 * existing {@code /batches/{jobId}/status} endpoint. The finished video is stored as a
 * {@link GeneratedAsset} with {@code poseId="video"} and {@code category="video"}.
 */
@Service
public class VideoGenerationService {

    private static final String VIDEO_POSE = "video";
    private static final String VIDEO_CATEGORY = "video";

    private final KlingAdapter videoClient;
    private final AssetGenerationJobRepository jobRepository;
    private final GeneratedAssetRepository generatedAssetRepository;
    private final CloudinaryService cloudinaryService;
    private final ProjectRepository projectRepository;

    public VideoGenerationService(KlingAdapter videoClient,
                                  AssetGenerationJobRepository jobRepository,
                                  GeneratedAssetRepository generatedAssetRepository,
                                  CloudinaryService cloudinaryService,
                                  ProjectRepository projectRepository) {
        this.videoClient = videoClient;
        this.jobRepository = jobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.cloudinaryService = cloudinaryService;
        this.projectRepository = projectRepository;
    }

    /**
     * Validates inputs, resolves the source try-on images, creates a tracking job, and kicks off
     * async generation. Fails fast (before the job is created) if the project or the required
     * front/back images are missing.
     *
     * @param imageJobId      the image-generation job that produced the try-on assets
     * @param productId       which product within that job to animate
     * @param folderId        the project/folder the video belongs to
     * @param durationSeconds desired video length (3–15); null uses the model default
     * @param prompt          optional creative prompt; null uses the default turntable prompt
     */
    public AssetGenerationJob submitVideoGeneration(String imageJobId,
                                                    String productId,
                                                    Long folderId,
                                                    Integer durationSeconds,
                                                    String prompt) {
        if (!projectRepository.existsById(folderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found: " + folderId);
        }

        Map<String, String> poseUrls = resolvePoseUrls(imageJobId, productId);
        String frontUrl = poseUrls.get("front");
        String backUrl = poseUrls.get("back");
        if (frontUrl == null || backUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot generate video: missing front and/or back try-on image for product '"
                            + productId + "' in job '" + imageJobId + "'");
        }

        List<String> references = new ArrayList<>();
        String sideUrl = poseUrls.get("side");
        if (sideUrl != null) {
            references.add(sideUrl);
        }

        AssetGenerationJob job = createJob(folderId);
        runVideoGeneration(job.getJobId(), productId, folderId, frontUrl, backUrl, references,
                durationSeconds, prompt);
        return job;
    }

    private AssetGenerationJob createJob(Long folderId) {
        String jobId = UUID.randomUUID().toString();
        String runId = "video_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // One unit of work: the single video.
        return jobRepository.save(new AssetGenerationJob(jobId, runId, null, 1, folderId));
    }

    @Async
    public void runVideoGeneration(String jobId,
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

            byte[] video = videoClient.generate(frontUrl, backUrl, referenceUrls, durationSeconds, prompt);

            String publicId = "bioracer/" + folderId + "/" + productId + "/video";
            CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadVideo(video, publicId);

            GeneratedAsset asset = new GeneratedAsset(
                    projectRepository.getReferenceById(folderId),
                    jobId,
                    productId,
                    VIDEO_POSE,
                    VIDEO_CATEGORY,
                    uploadResult.secureUrl(),
                    uploadResult.thumbnailUrl(),
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

    /** Maps poseId (front/back/side) to the Cloudinary delivery URL of its try-on image. */
    private Map<String, String> resolvePoseUrls(String imageJobId, String productId) {
        return generatedAssetRepository.findByJobId(imageJobId).stream()
                .filter(a -> productId.equals(a.getProductId()))
                .filter(a -> a.getPoseId() != null && a.getSecureUrl() != null)
                .collect(java.util.stream.Collectors.toMap(
                        GeneratedAsset::getPoseId,
                        GeneratedAsset::getSecureUrl,
                        (existing, ignored) -> existing));
    }
}
