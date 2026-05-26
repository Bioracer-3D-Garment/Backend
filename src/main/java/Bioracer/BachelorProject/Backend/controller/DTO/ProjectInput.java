package Bioracer.BachelorProject.Backend.controller.DTO;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ProjectInput(
                @NotBlank(message = "Project name is required.") String name,
                String coverImage,
                List<String> gallery
                ) {
}