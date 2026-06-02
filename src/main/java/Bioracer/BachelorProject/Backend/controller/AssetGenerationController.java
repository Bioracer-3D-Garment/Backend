package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.BatchStatusResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ErrorResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.GeneratedAssetResponse;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchStatus;
import Bioracer.BachelorProject.Backend.pipeline.repository.BatchJobRepository;
import Bioracer.BachelorProject.Backend.pipeline.services.BatchService;
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
    private final BatchService batchService;
    private final BatchJobRepository jobRepository;
    private final GeneratedAssetRepository generatedAssetRepository;

    public AssetGenerationController(BatchService batchService,
                                     BatchJobRepository jobRepository,
                                     GeneratedAssetRepository generatedAssetRepository, Bioracer.BachelorProject.Backend.controller.ExceptionHandlers exceptionHandlers) {
        this.batchService = batchService;
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
        BatchJob job;
        try {
            job = batchService.submitBatch(frontDesign, backDesign, modelId, folderId);
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
    public ResponseEntity<BatchStatusResponse> getStatus(@PathVariable String jobId) {
        BatchJob job = requireJob(jobId);

        List<GeneratedAssetResponse> assets = null;
        BatchStatus status = job.getStatus();
        if (status == BatchStatus.DONE || status == BatchStatus.PARTIAL) {
            assets = generatedAssetRepository.findByJobId(jobId).stream()
                    .map(GeneratedAssetResponse::from)
                    .toList();
        }

        return ResponseEntity.ok(new BatchStatusResponse(
                job.getJobId(),
                job.getStatus().name(),
                job.getCompletedCount(),
                job.getTotalCount(),
                job.getUploadedCount(),
                job.getFailedItems(),
                assets));
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{jobId}/download")
    public ResponseEntity<?> downloadJob(@PathVariable String jobId) throws IOException {
        BatchJob job = requireJob(jobId);

        if (job.getStatus() != BatchStatus.DONE && job.getStatus() != BatchStatus.PARTIAL) {
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

    private BatchJob requireJob(String jobId) {
        return jobRepository.findById(jobId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Batch job not found: " + jobId));
    }
}