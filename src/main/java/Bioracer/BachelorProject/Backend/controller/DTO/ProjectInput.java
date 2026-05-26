package Bioracer.BachelorProject.Backend.controller.DTO;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProjectInput(
        @NotBlank(message = "Project name is required.") String name,

        @Pattern(regexp = "^https://res\\.cloudinary\\.com/.+", message = "Cover image must be a valid Cloudinary secure URL.") String coverImage,

        List<@Pattern(regexp = "^https://res\\.cloudinary\\.com/.+", message = "Each gallery image must be a valid Cloudinary secure URL.") String> images) {
}