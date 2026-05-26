package Bioracer.BachelorProject.Backend.controller.DTO;

import java.util.List;

public record ZipValidationErrorResponse(
        int status,
        String error,
        String message,
        List<GarmentError> garmentErrors
) {
    public record GarmentError(String garment, List<String> found, List<String> missing) {}
}
