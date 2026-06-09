package Bioracer.BachelorProject.Backend.controller.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to generate a turntable video from a product's already-generated Fashn try-on images.
 *
 * @param imageJobId      the image-generation job ({@code /batches}) whose front/back/side assets to animate
 * @param productId       which product within that job to animate
 * @param folderId        the project/folder the video belongs to
 * @param durationSeconds desired length in seconds (3–15); null uses the model default (5)
 * @param prompt          optional creative prompt; null uses the default turntable prompt
 */
public record VideoGenerationRequest(
        String imageJobId,
        String productId,
        Long folderId,
        @Schema(nullable = true, minimum = "3", maximum = "15") Integer durationSeconds,
        @Schema(nullable = true) String prompt) {
}
