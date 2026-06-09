package Bioracer.BachelorProject.Backend.controller.DTO;

import Bioracer.BachelorProject.Backend.pipeline.models.FailedItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AssetGenerationStatusResponse(
        String jobId,
        @Schema(allowableValues = {"PENDING", "RUNNING", "DONE", "PARTIAL", "FAILED"})
        String status,
        int completed,
        int total,
        int uploadedCount,
        List<FailedItem> failedItems,
        @Schema(nullable = true)
        List<GeneratedAssetResponse> assets,
        @Schema(nullable = true, description = "Failure reason when status is FAILED (e.g. video pipeline error); null otherwise.")
        String errorMessage) {}
