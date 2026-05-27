package Bioracer.BachelorProject.Backend.pipeline.utils;

import java.util.Map;

public record GarmentZipEntry(
        String garmentName,
        String category,                 // "upper_body" or "lower_body" from category.txt
        Map<String, byte[]> angleImages  // keys: "front", "back", "side"
) {}
