package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.BatchStatusResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.BatchSubmitResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ErrorResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.GeneratedAssetResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ZipValidationErrorResponse;
import Bioracer.BachelorProject.Backend.exception.ZipValidationException;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJobRepository;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchStatus;
import Bioracer.BachelorProject.Backend.pipeline.services.BatchService;
import Bioracer.BachelorProject.Backend.pipeline.utils.GarmentZipEntry;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

        List<GarmentZipEntry> garments = parseGarmentZip(garmentZip);

        String normalizedGender = gender.trim().toLowerCase();

        // Validate server-side pose directories before creating the job
        for (String angle : List.of("front", "back", "side")) {
            try {
                batchService.resolvePoseFileForAngle(normalizedGender, angle);
            } catch (IllegalStateException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }

        int total = garments.size() * 3;
        BatchJob job = batchService.createJob(total, folderId);
        batchService.runBatch(job.getJobId(), garments, normalizedGender);

        log.info("Submitted batch job {} — {} garments, {} combinations, gender={}",
                job.getJobId(), garments.size(), total, normalizedGender);
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

    // ---- ZIP parsing ----

    /**
     * Parses a ZIP with the structure:
     *   GarmentName/front.jpg
     *   GarmentName/back.jpg
     *   GarmentName/side.jpg
     *   GarmentName/category.txt   (contains "upper_body" or "lower_body")
     *
     * Files at the top level or deeper than one subfolder are ignored.
     * Throws ZipValidationException if any garment subfolder is missing required files.
     */
    private List<GarmentZipEntry> parseGarmentZip(MultipartFile garmentZip) throws IOException {
        Map<String, Map<String, byte[]>> angleImages = new LinkedHashMap<>();
        Map<String, String> categories = new LinkedHashMap<>();

        try (ZipInputStream zis = new ZipInputStream(garmentZip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) { zis.closeEntry(); continue; }

                String path = entry.getName().replace('\\', '/');
                // Skip macOS metadata and hidden files
                if (path.startsWith("__MACOSX/") || path.contains("/.")) {
                    zis.closeEntry();
                    continue;
                }

                String[] parts = path.split("/");
                // Need at least garmentFolder/filename — skip root-level files
                if (parts.length < 2) { zis.closeEntry(); continue; }

                // Use the immediate parent folder as the garment name regardless of nesting depth.
                // Handles both GarmentName/file.png and WrapperFolder/GarmentName/file.png.
                String garmentName   = parts[parts.length - 2];
                String lowerFilename = parts[parts.length - 1].toLowerCase();

                if (lowerFilename.equals("category.txt")) {
                    String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8)
                            .trim().toLowerCase();
                    categories.put(garmentName, content);
                } else {
                    // Accept front/back/side with any image extension (.jpg, .jpeg, .png)
                    int dot = lowerFilename.lastIndexOf('.');
                    if (dot > 0) {
                        String basename  = lowerFilename.substring(0, dot);
                        String extension = lowerFilename.substring(dot + 1);
                        boolean isImage  = extension.equals("jpg") || extension.equals("jpeg")
                                        || extension.equals("png");
                        if (isImage && (basename.equals("front") || basename.equals("back")
                                     || basename.equals("side"))) {
                            angleImages
                                    .computeIfAbsent(garmentName, k -> new LinkedHashMap<>())
                                    .put(basename, zis.readAllBytes());
                        }
                    }
                }

                zis.closeEntry();
            }
        }

        Set<String> allGarments = new LinkedHashSet<>(angleImages.keySet());
        allGarments.addAll(categories.keySet());

        if (allGarments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No garment subfolders found in ZIP");
        }

        // Validate each garment has all four required files with valid content
        List<ZipValidationErrorResponse.GarmentError> errors = new ArrayList<>();

        for (String garmentName : allGarments) {
            Map<String, byte[]> poses    = angleImages.getOrDefault(garmentName, Map.of());
            String              category = categories.get(garmentName);

            List<String> found   = new ArrayList<>();
            List<String> missing = new ArrayList<>();

            for (String pose : List.of("front", "back", "side")) {
                if (poses.containsKey(pose)) found.add(pose + ".jpg");
                else                          missing.add(pose + ".jpg");
            }

            if (category == null) {
                missing.add("category.txt");
            } else {
                found.add("category.txt");
                if (!category.equals("upper_body") && !category.equals("lower_body")) {
                    missing.add("category.txt (must be 'upper_body' or 'lower_body', got: '"
                            + category + "')");
                }
            }

            if (!missing.isEmpty()) {
                errors.add(new ZipValidationErrorResponse.GarmentError(garmentName, found, missing));
            }
        }

        if (!errors.isEmpty()) {
            throw new ZipValidationException(new ZipValidationErrorResponse(
                    400,
                    "Bad Request",
                    "ZIP validation failed: one or more garment folders are missing required files",
                    errors));
        }

        return allGarments.stream()
                .map(name -> new GarmentZipEntry(name, categories.get(name), angleImages.get(name)))
                .toList();
    }

    private BatchJob requireJob(String jobId) {
        return jobRepository.findById(jobId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Batch job not found: " + jobId));
    }
}
