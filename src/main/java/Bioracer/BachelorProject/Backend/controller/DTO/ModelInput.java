package Bioracer.BachelorProject.Backend.controller.DTO;

import Bioracer.BachelorProject.Backend.model.Gender;

public record ModelInput(String name,
        String coverImage,
        String front,
        String back,
        String side,
        Gender gender) {
}
