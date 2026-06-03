package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.VideoGenerationRequest;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationJob;
import Bioracer.BachelorProject.Backend.pipeline.services.VideoGenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turntable video generation (fal.ai Kling v3 Pro image-to-video).
 *
 * <p>Generation runs asynchronously and is tracked via the shared batch-job machinery, so poll
 * progress and retrieve the finished video through the existing {@code GET /batches/{jobId}/status}
 * endpoint — the returned asset has {@code poseId="video"} and its {@code secureUrl} is the MP4.
 */
@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoGenerationService videoGenerationService;

    public VideoController(VideoGenerationService videoGenerationService) {
        this.videoGenerationService = videoGenerationService;
    }

    /**
     * POST /videos
     *
     * Animates a product's existing Fashn try-on images (front → start frame, back → end frame,
     * side → extra reference) into a turntable video. Returns the tracking job id.
     */
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> submitVideo(@RequestBody VideoGenerationRequest request) {
        if (request.imageJobId() == null || request.imageJobId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageJobId is required");
        }
        if (request.productId() == null || request.productId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        if (request.folderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "folderId is required");
        }

        AssetGenerationJob job = videoGenerationService.submitVideoGeneration(
                request.imageJobId(),
                request.productId(),
                request.folderId(),
                request.durationSeconds(),
                request.prompt());

        return ResponseEntity.accepted().body("Video job has been accepted: " + job.getJobId());
    }
}
