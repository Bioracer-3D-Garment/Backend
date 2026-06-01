package Bioracer.BachelorProject.Backend.util;

import java.util.List;

import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;

/**
 * Static helpers for building domain objects and DTOs used across tests.
 */
public final class TestDataFactory {

    public static final String COVER_URL = "https://res.cloudinary.com/demo/image/upload/v1/cover.jpg";
    public static final List<String> GALLERY_URLS = List.of(
            "https://res.cloudinary.com/demo/image/upload/v1/gallery-1.jpg",
            "https://res.cloudinary.com/demo/image/upload/v1/gallery-2.jpg");

    private TestDataFactory() {
    }

    public static User user(String email) {
        return new User("Test", "User", email, "hashed-password", Role.USER);
    }

    public static ProjectInput projectInput(String name) {
        return new ProjectInput(name, COVER_URL, GALLERY_URLS);
    }

    public static Project project(String name, User owner) {
        return new Project(name, owner, COVER_URL, GALLERY_URLS);
    }
}