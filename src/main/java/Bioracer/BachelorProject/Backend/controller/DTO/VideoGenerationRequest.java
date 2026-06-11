package Bioracer.BachelorProject.Backend.controller.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

public record VideoGenerationRequest(
                String imageJobId,
                String productId,
                Long folderId,
                @Schema(nullable = true, minimum = "3", maximum = "15") Integer durationSeconds,
                @Schema(nullable = true) String prompt) {
}
