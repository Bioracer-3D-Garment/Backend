package Bioracer.BachelorProject.Backend.controller.DTO;

import java.util.List;

public record ProjectAssetsResponse(
        Long projectId,
        long totalCount,
        int page,
        int size,
        List<GeneratedAssetResponse> assets) {}
