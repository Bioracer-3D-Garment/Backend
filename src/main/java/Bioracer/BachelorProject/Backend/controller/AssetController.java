package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.ErrorResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ProjectAssetsResponse;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.service.AssetService;
import Bioracer.BachelorProject.Backend.service.JwtService;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class AssetController {

    private static final Logger log = LoggerFactory.getLogger(AssetController.class);

    private final AssetService assetService;
    private final JwtService jwtService;

    public AssetController(AssetService assetService, JwtService jwtService) {
        this.assetService = assetService;
        this.jwtService = jwtService;
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assets retrieved",
            content = @Content(schema = @Schema(implementation = ProjectAssetsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Caller does not own the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/projects/{projectId}/assets")
    public ResponseEntity<ProjectAssetsResponse> getProjectAssets(
            @PathVariable Long projectId,
            @RequestParam(required = false) String jobId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {

        Long userId = extractIdFromHeader(authHeader);
        AssetService.ProjectAssetsPage result =
                assetService.getProjectAssets(projectId, userId, jobId, category, page, size);

        return ResponseEntity.ok(new ProjectAssetsResponse(
                result.projectId(),
                result.totalCount(),
                result.page(),
                result.size(),
                result.assets()));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Asset deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Caller does not own the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Asset not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/assets/{assetId}")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable Long assetId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {

        Long userId = extractIdFromHeader(authHeader);
        assetService.deleteAsset(assetId, userId);
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ZIP archive of all project assets",
            content = @Content(mediaType = "application/octet-stream",
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Caller does not own the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found or has no assets",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/projects/{projectId}/assets/download")
    public ResponseEntity<byte[]> downloadProjectAssets(
            @PathVariable Long projectId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) throws IOException {

        Long userId = extractIdFromHeader(authHeader);
        List<GeneratedAsset> assets = assetService.getProjectAssetsForDownload(projectId, userId);

        if (assets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No assets found for project: " + projectId);
        }

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
                .header("Content-Disposition",
                        "attachment; filename=\"project-" + projectId + ".zip\"")
                .body(baos.toByteArray());
    }

    private Long extractIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }
        return jwtService.extractId(authHeader.substring(7));
    }
}
