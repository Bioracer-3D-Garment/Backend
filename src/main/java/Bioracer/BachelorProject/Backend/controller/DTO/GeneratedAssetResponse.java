package Bioracer.BachelorProject.Backend.controller.DTO;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;

public record GeneratedAssetResponse(
        Long id,
        Long projectId,
        String jobId,
        String productId,
        String poseId,
        String category,
        String secureUrl,
        String thumbnailUrl,
        String createdAt) {

    public static GeneratedAssetResponse from(GeneratedAsset entity) {
        return new GeneratedAssetResponse(
                entity.getId(),
                entity.getProject().getId(),
                entity.getJobId(),
                entity.getProductId(),
                entity.getPoseId(),
                entity.getCategory(),
                entity.getSecureUrl(),
                entity.getThumbnailUrl(),
                entity.getCreatedAt().toString());
    }
}
