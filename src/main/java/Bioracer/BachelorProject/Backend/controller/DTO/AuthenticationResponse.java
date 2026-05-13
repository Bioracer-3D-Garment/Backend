package Bioracer.BachelorProject.Backend.controller.DTO;

import Bioracer.BachelorProject.Backend.model.Role;

public record AuthenticationResponse(
        String message,
        String token,
        String email,
        String firstName,
        String lastName,
        Role role,
        Long userId

) {
}
