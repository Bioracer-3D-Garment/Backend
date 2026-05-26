package Bioracer.BachelorProject.Backend.pipeline.utils;

public record CatalogProduct(
        String productId,
        String category,           // Fashn.ai category: upper_body, lower_body
        byte[] garmentImageBytes
) {}
