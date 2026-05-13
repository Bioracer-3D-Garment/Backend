package Bioracer.BachelorProject.Backend.controller.DTO;

public record UserInput(
        String password,
        String firstName,
        String lastName,
        String email,
        String role) {
}
