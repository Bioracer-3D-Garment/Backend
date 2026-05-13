package Bioracer.BachelorProject.Backend.controller.DTO;

public record AuthenticationRequest(
        String email,
        String password) {
}
