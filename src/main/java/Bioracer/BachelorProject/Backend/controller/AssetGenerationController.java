package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.AssetGenerationStatusResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ErrorResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.GeneratedAssetResponse;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationJob;
import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationStatus;
import Bioracer.BachelorProject.Backend.pipeline.repository.AssetGenerationJobRepository;
import Bioracer.BachelorProject.Backend.pipeline.services.AssetGenerationService;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/batches")
public class AssetGenerationController {
    private final AssetGenerationService assetGenerationService;
    private final AssetGenerationJobRepository jobRepository;
    private final GeneratedAssetRepository generatedAssetRepository;

    public AssetGenerationController(AssetGenerationService assetGenerationService,
                                     AssetGenerationJobRepository jobRepository,
                                     GeneratedAssetRepository generatedAssetRepository) {
        this.assetGenerationService = assetGenerationService;
        this.jobRepository = jobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
    }

    /**
     * POST /batches
     *
     * Accepts multipart/form-data with separate garment views:
     *   frontDesign — front view of the clothing item
     *   backDesign  — back view of the clothing item
     *   modelId     — model ID (required)
     *   folderId    — project ID (required)
     */
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> submitBatch(
            @RequestParam("frontDesign") MultipartFile frontDesign,
            @RequestParam("backDesign") MultipartFile backDesign,
            @RequestParam("modelId") Long modelId,
            @RequestParam("folderId") Long folderId) {
        AssetGenerationJob job;
        try {
            job = assetGenerationService.submitAssetGeneration(frontDesign, backDesign, modelId, folderId);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process the uploaded garment images", e);
        } catch (Exception e) {
            throw e;
        }

        return ResponseEntity.accepted().body("Job has been accepted: " + job.getJobId());
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{jobId}/status")
    public ResponseEntity<AssetGenerationStatusResponse> getStatus(@PathVariable String jobId) {
        AssetGenerationJob job = requireJob(jobId);

        List<GeneratedAssetResponse> assets = null;
        AssetGenerationStatus status = job.getStatus();
        if (status == AssetGenerationStatus.DONE || status == AssetGenerationStatus.PARTIAL) {
            assets = generatedAssetRepository.findByJobId(jobId).stream()
                    .map(GeneratedAssetResponse::from)
                    .toList();
        }

        return ResponseEntity.ok(new AssetGenerationStatusResponse(
                job.getJobId(),
                job.getStatus().name(),
                job.getCompletedCount(),
                job.getTotalCount(),
                job.getUploadedCount(),
                job.getFailedItems(),
                assets,
                job.getErrorMessage()));
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{jobId}/download")
    public ResponseEntity<?> downloadJob(@PathVariable String jobId) throws IOException {
        AssetGenerationJob job = requireJob(jobId);

        if (job.getStatus() != AssetGenerationStatus.DONE && job.getStatus() != AssetGenerationStatus.PARTIAL) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(409, "Conflict", "Job not finished yet"));
        }

        List<GeneratedAsset> assets = generatedAssetRepository.findByJobId(jobId);

        RestClient restClient = RestClient.create();
        ByteArrayOutputStream zipArchiveBuffer = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(zipArchiveBuffer)) {
            for (GeneratedAsset asset : assets) {
                String entryName = asset.getProductId() + "_" + asset.getPoseId() + ".jpg";
                try {
                    byte[] bytes = restClient.get()
                            .uri(URI.create(asset.getSecureUrl()))
                            .retrieve()
                            .body(byte[].class);
                    if (bytes != null) {
                        zipOutputStream.putNextEntry(new ZipEntry(entryName));
                        zipOutputStream.write(bytes);
                        zipOutputStream.closeEntry();
                    }
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to download asset: " + asset.getId(), e);
                }
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + jobId + ".zip\"")
            .body(zipArchiveBuffer.toByteArray());
    }

    private AssetGenerationJob requireJob(String jobId) {
        return jobRepository.findById(jobId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Asset generation job not found: " + jobId));
    }
}