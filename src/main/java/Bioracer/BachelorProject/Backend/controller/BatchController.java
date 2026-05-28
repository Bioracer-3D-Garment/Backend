package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.BatchStatusResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.BatchSubmitResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ErrorResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.GeneratedAssetResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ZipValidationErrorResponse;
import Bioracer.BachelorProject.Backend.exception.ZipValidationException;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchStatus;
import Bioracer.BachelorProject.Backend.pipeline.repository.BatchJobRepository;
import Bioracer.BachelorProject.Backend.pipeline.services.BatchService;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final BatchService batchService;
    private final BatchJobRepository jobRepository;
    private final GeneratedAssetRepository generatedAssetRepository;

    public BatchController(BatchService batchService,
                           BatchJobRepository jobRepository,
                           GeneratedAssetRepository generatedAssetRepository) {
        this.batchService = batchService;
        this.jobRepository = jobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
    }

    /**
     * POST /batches
     *
     * Accepts multipart/form-data with a single ZIP archive:
     *   garmentZip  — ZIP with GarmentName/{front,back,side}.jpg + category.txt per subfolder
     *   gender      — "male" | "female"
     *   folderId    — project ID (required)
     *
     * Every garment subfolder must contain front.jpg, back.jpg, side.jpg, and category.txt
     * (containing "upper_body" or "lower_body"). Missing files → 400 with garmentErrors detail.
     */
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Batch job accepted",
            content = @Content(schema = @Schema(implementation = BatchSubmitResponse.class))),
        @ApiResponse(responseCode = "400", description = "ZIP validation failed or missing parameters",
            content = @Content(schema = @Schema(implementation = ZipValidationErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Project not owned by caller",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchSubmitResponse> submitBatch(
            @RequestPart("garmentZip")  MultipartFile garmentZip,
            @RequestParam("gender")     String gender,
            @RequestParam("folderId")   Long folderId) throws IOException {

        BatchJob job = batchService.submitBatch(garmentZip, gender, folderId);

        return ResponseEntity.accepted().body(new BatchSubmitResponse(job.getJobId()));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job status",
            content = @Content(schema = @Schema(implementation = BatchStatusResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Job not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ZIP archive of generated images",
            content = @Content(mediaType = "application/octet-stream",
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Job not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Job not finished yet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (GeneratedAsset asset : assets) {
                String entryName = asset.getProductId() + "_" + asset.getPoseId() + ".jpg";
                try {
                    byte[] bytes = restClient.get()
                            .uri(URI.create(asset.getSecureUrl()))
                            .retrieve()
                            .body(byte[].class);
                    if (bytes != null) {
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.write(bytes);
                        zos.closeEntry();
                    }
                } catch (Exception e) {
                    log.warn("Skipping asset id={} url={} — fetch failed: {}",
                            asset.getId(), asset.getSecureUrl(), e.getMessage());
                }
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + jobId + ".zip\"")
                .body(baos.toByteArray());
    }

    private BatchJob requireJob(String jobId) {
        return jobRepository.findById(jobId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Batch job not found: " + jobId));
    }
}
