package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJobRepository;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchStatus;
import Bioracer.BachelorProject.Backend.pipeline.services.BatchService;
import Bioracer.BachelorProject.Backend.pipeline.utils.CatalogProduct;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/batches")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final BatchService batchService;
    private final BatchJobRepository jobRepository;

    public BatchController(BatchService batchService, BatchJobRepository jobRepository) {
        this.batchService = batchService;
        this.jobRepository = jobRepository;
    }

    /**
     * POST /batches
     *
     * Accepts multipart/form-data. Garments via one of two mutually exclusive options:
     *   Option A — individual files:
     *     garmentFiles      (repeated MultipartFile)
     *     garmentNames      (repeated String, same order as garmentFiles)
     *     garmentCategories (repeated String: "top" | "bottom", same order)
     *   Option B — ZIP archive:
     *     catalog           (single MultipartFile, ZIP with tops/ and bottoms/ folders)
     *
     * Common fields:
     *   gender    ("male" | "female") — all pose images in {poses-dir}/{gender}/ are used
     *   folderId  (Long)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitBatch(
            @RequestPart(value = "garmentFiles",      required = false) List<MultipartFile> garmentFiles,
            @RequestParam(value = "garmentNames",     required = false) List<String> garmentNames,
            @RequestParam(value = "garmentCategories",required = false) List<String> garmentCategories,
            @RequestPart(value = "catalog",           required = false) MultipartFile catalog,
            @RequestParam("gender")                   String gender,
            @RequestParam(value = "folderId",         required = false) Long folderId) throws IOException {

        // --- parse garments ---
        List<CatalogProduct> products;
        boolean hasFiles   = garmentFiles != null && !garmentFiles.isEmpty();
        boolean hasCatalog = catalog != null && !catalog.isEmpty();

        if (hasFiles && hasCatalog) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide either garmentFiles or catalog — not both");
        }
        if (!hasFiles && !hasCatalog) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide either garmentFiles or catalog");
        }

        if (hasFiles) {
            products = parseIndividualGarments(garmentFiles, garmentNames, garmentCategories);
        } else {
            products = parseZipCatalog(catalog);
        }

        if (products.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid garments found");
        }

        // --- validate gender and resolve pose files per distinct garment category ---
        String normalizedGender = gender.trim().toLowerCase();
        products.stream().map(CatalogProduct::category).distinct().forEach(category -> {
            List<java.nio.file.Path> poses = batchService.resolvePoseFiles(normalizedGender, category);
            if (poses.isEmpty()) {
                String folder = BatchService.categoryToFolder(category);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No pose images found for gender='" + normalizedGender +
                        "' category='" + folder + "'. Expected images in {poses-dir}/" +
                        normalizedGender + "/" + folder + "/");
            }
        });

        // Total = sum of pose count per product (tops and bottoms may have different counts)
        int total = products.stream()
                .mapToInt(p -> batchService.resolvePoseFiles(normalizedGender, p.category()).size())
                .sum();

        BatchJob job = batchService.createJob(total, folderId);
        batchService.runBatch(job.getJobId(), products, normalizedGender);

        log.info("Submitted batch job {} — {} products, {} combinations, gender={}",
                job.getJobId(), products.size(), total, normalizedGender);
        return ResponseEntity.accepted().body(Map.of("jobId", job.getJobId()));
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String jobId) {
        BatchJob job = requireJob(jobId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId",       job.getJobId());
        body.put("status",      job.getStatus().name());
        body.put("completed",   job.getCompletedCount());
        body.put("total",       job.getTotalCount());
        body.put("failedItems", job.getFailedItems());

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{jobId}/images/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String jobId, @PathVariable String filename) throws IOException {
        BatchJob job = requireJob(jobId);
        Path outputPath = Paths.get(job.getOutputPath());

        if (!Files.isDirectory(outputPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Output directory not found for job: " + jobId);
        }

        // find a file matching the name (with any image extension)
        Path imageFile;
        try (Stream<Path> stream = Files.list(outputPath)) {
            imageFile = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        int dot = name.lastIndexOf('.');
                        String base = (dot > 0) ? name.substring(0, dot) : name;
                        return base.equals(filename) && BatchService.isImageFile(name);
                    })
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Image not found: " + filename + " in job " + jobId));
        }

        byte[] bytes = Files.readAllBytes(imageFile);
        MediaType contentType = detectMediaType(imageFile.getFileName().toString());
        return ResponseEntity.ok().contentType(contentType).body(bytes);
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<StreamingResponseBody> downloadJob(@PathVariable String jobId) {
        BatchJob job = requireJob(jobId);

        if (job.getStatus() != BatchStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job not finished: " + jobId);
        }

        Path outputPath = Paths.get(job.getOutputPath());
        if (!Files.isDirectory(outputPath)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Output directory not found for job: " + jobId);
        }

        StreamingResponseBody stream = out -> {
            try (ZipOutputStream zos = new ZipOutputStream(out);
                 Stream<Path> files = Files.walk(outputPath)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    String entryName = outputPath.relativize(file).toString();
                    try {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to add file to ZIP: " + entryName, e);
                    }
                });
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + jobId + ".zip\"")
                .body(stream);
    }

    // ---- parsing helpers ----

    private List<CatalogProduct> parseIndividualGarments(List<MultipartFile> files,
                                                          List<String> names,
                                                          List<String> categories) throws IOException {
        if (categories == null || categories.size() != files.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "garmentCategories is required for each file — send 'top' or 'bottom' " +
                    "for each garmentFiles entry (same order, same count)");
        }

        List<CatalogProduct> products = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            // fall back to original filename (without extension) if garmentNames not sent
            String name = (names != null && i < names.size() && !names.get(i).isBlank())
                    ? names.get(i).trim()
                    : stripExtension(files.get(i).getOriginalFilename());
            String category = mapCategory(categories.get(i).trim());
            byte[] bytes    = files.get(i).getBytes();
            products.add(new CatalogProduct(name, category, bytes));
        }
        return products;
    }

    private static String stripExtension(String filename) {
        if (filename == null) return "garment";
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }

    /**
     * Parses a ZIP with the structure:
     *   tops/&lt;productId&gt;/&lt;image.png&gt;
     *   bottoms/&lt;productId&gt;/&lt;image.png&gt;
     * The top-level folder name determines category; the subfolder name is the product ID.
     */
    private List<CatalogProduct> parseZipCatalog(MultipartFile catalog) throws IOException {
        Map<String, CatalogProduct> byProductId = new LinkedHashMap<>();

        try (ZipInputStream zis = new ZipInputStream(catalog.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) { zis.closeEntry(); continue; }

                String path  = entry.getName().replace('\\', '/');
                String[] parts = path.split("/");

                // look for tops|bottoms / productId / filename at any nesting level
                String categoryFolder = null;
                String productId      = null;
                String filename       = null;

                for (int i = 0; i <= parts.length - 3; i++) {
                    if (parts[i].equalsIgnoreCase("tops") || parts[i].equalsIgnoreCase("bottoms")) {
                        categoryFolder = parts[i];
                        productId      = parts[i + 1];
                        filename       = parts[parts.length - 1];
                        break;
                    }
                }

                if (categoryFolder == null || !BatchService.isImageFile(filename)) {
                    zis.closeEntry();
                    continue;
                }

                // take first image per product
                if (!byProductId.containsKey(productId)) {
                    String category = mapCategory(
                            categoryFolder.equalsIgnoreCase("tops") ? "top" : "bottom");
                    byProductId.put(productId, new CatalogProduct(productId, category, zis.readAllBytes()));
                }
                zis.closeEntry();
            }
        }

        return new ArrayList<>(byProductId.values());
    }

    private static String mapCategory(String frontendCategory) {
        return switch (frontendCategory.toLowerCase()) {
            case "top"    -> "upper_body";
            case "bottom" -> "lower_body";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown garment category: '" + frontendCategory + "'. Use 'top' or 'bottom'");
        };
    }

    private BatchJob requireJob(String jobId) {
        return jobRepository.findById(jobId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Batch job not found: " + jobId));
    }

    private static MediaType detectMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
