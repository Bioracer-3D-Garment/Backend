package Bioracer.BachelorProject.Backend.service;

import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@DisplayName("Project Service Tests")
public class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // Clean up
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser = new User("testuser", "Test User", "test@example.com", 
                passwordEncoder.encode("password123"), Role.USER);
        anotherUser = new User("anotheruser", "Another User", "another@example.com", 
                passwordEncoder.encode("password123"), Role.USER);

        testUser = userRepository.save(testUser);
        anotherUser = userRepository.save(anotherUser);
    }

    @Test
    @DisplayName("Create project with name and cover image")
    void testCreateProjectWithCoverImage() {
        // Given
        ProjectInput input = new ProjectInput("My Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                null);

        // When
        Project createdProject = projectService.createProject(input, testUser.getId());

        // Then
        assertNotNull(createdProject.getId());
        assertEquals("My Project", createdProject.getName());
        assertEquals(testUser.getId(), createdProject.getUser().getId());
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                createdProject.getCoverImage());
        assertNotNull(createdProject.getGallery());
        assertTrue(createdProject.getGallery().isEmpty());
    }

    @Test
    @DisplayName("Create project with gallery")
    void testCreateProjectWithGallery() {
        // Given
        List<String> galleryImages = new ArrayList<>();
        galleryImages.add("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image1.jpg");
        galleryImages.add("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image2.jpg");
        galleryImages.add("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image3.jpg");

        ProjectInput input = new ProjectInput("Gallery Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                galleryImages);

        // When
        Project createdProject = projectService.createProject(input, testUser.getId());

        // Then
        assertNotNull(createdProject.getId());
        assertEquals("Gallery Project", createdProject.getName());
        assertEquals(3, createdProject.getGallery().size());
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image1.jpg", 
                createdProject.getGallery().get(0));
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image2.jpg", 
                createdProject.getGallery().get(1));
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image3.jpg", 
                createdProject.getGallery().get(2));
    }

    @Test
    @DisplayName("Update project with new gallery")
    void testUpdateProjectGallery() {
        // Given - Create initial project
        ProjectInput initialInput = new ProjectInput("Original Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                new ArrayList<>(List.of("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/old.jpg")));
        Project project = projectService.createProject(initialInput, testUser.getId());
        Long projectId = project.getId();

        // Given - Update with new gallery
        List<String> newGallery = new ArrayList<>();
        newGallery.add("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/new1.jpg");
        newGallery.add("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/new2.jpg");
        ProjectInput updateInput = new ProjectInput("Original Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                newGallery);

        // When
        Project updatedProject = projectService.updateProjectDetails(projectId, updateInput, testUser.getId());

        // Then
        assertEquals(2, updatedProject.getGallery().size());
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/new1.jpg", 
                updatedProject.getGallery().get(0));
    }

    @Test
    @DisplayName("Update project with empty gallery")
    void testClearGallery() {
        // Given - Create initial project with gallery
        ProjectInput initialInput = new ProjectInput("Original Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                new ArrayList<>(List.of(
                        "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image1.jpg",
                        "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image2.jpg")));
        Project project = projectService.createProject(initialInput, testUser.getId());
        Long projectId = project.getId();

        // Given - Update with empty gallery
        ProjectInput updateInput = new ProjectInput("Original Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                new ArrayList<>());

        // When
        Project updatedProject = projectService.updateProjectDetails(projectId, updateInput, testUser.getId());

        // Then
        assertNotNull(updatedProject.getGallery());
        assertTrue(updatedProject.getGallery().isEmpty());
    }

    @Test
    @DisplayName("Update project name and cover image")
    void testUpdateProjectNameAndCoverImage() {
        // Given
        ProjectInput initialInput = new ProjectInput("Original Name", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/old-cover.jpg", 
                null);
        Project project = projectService.createProject(initialInput, testUser.getId());
        Long projectId = project.getId();

        // When
        ProjectInput updateInput = new ProjectInput("Updated Name", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/new-cover.jpg", 
                null);
        Project updatedProject = projectService.updateProjectDetails(projectId, updateInput, testUser.getId());

        // Then
        assertEquals("Updated Name", updatedProject.getName());
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/new-cover.jpg", 
                updatedProject.getCoverImage());
    }

    @Test
    @DisplayName("Fetch project by ID and user ID")
    void testGetProjectById() {
        // Given
        ProjectInput input = new ProjectInput("Fetch Test", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                null);
        Project createdProject = projectService.createProject(input, testUser.getId());

        // When
        Project fetchedProject = projectService.getProjectById(createdProject.getId(), testUser.getId());

        // Then
        assertEquals(createdProject.getId(), fetchedProject.getId());
        assertEquals("Fetch Test", fetchedProject.getName());
    }

    @Test
    @DisplayName("Authorization check: user cannot access other user's project")
    void testUnauthorizedProjectAccess() {
        // Given
        ProjectInput input = new ProjectInput("Private Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                null);
        Project createdProject = projectService.createProject(input, testUser.getId());

        // When & Then
        assertThrows(UserException.class, 
                () -> projectService.getProjectById(createdProject.getId(), anotherUser.getId()),
                "Should throw UserException with 'Not authorized!' message");
    }

    @Test
    @DisplayName("Authorization check: user cannot update other user's project")
    void testUnauthorizedProjectUpdate() {
        // Given
        ProjectInput initialInput = new ProjectInput("Original Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                null);
        Project project = projectService.createProject(initialInput, testUser.getId());

        ProjectInput updateInput = new ProjectInput("Hacked Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/hacked-cover.jpg", 
                null);

        // When & Then
        assertThrows(UserException.class, 
                () -> projectService.updateProjectDetails(project.getId(), updateInput, anotherUser.getId()),
                "Should throw UserException when updating other user's project");
    }

    @Test
    @DisplayName("Create project with non-existent user")
    void testCreateProjectWithInvalidUser() {
        // Given
        ProjectInput input = new ProjectInput("Invalid User Project", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                null);

        // When & Then
        assertThrows(UserException.class, 
                () -> projectService.createProject(input, 99999L),
                "Should throw UserException when user does not exist");
    }

    @Test
    @DisplayName("Get all projects by user ID")
    void testGetAllProjectsByUserId() {
        // Given
        ProjectInput input1 = new ProjectInput("Project 1", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover1.jpg", 
                null);
        ProjectInput input2 = new ProjectInput("Project 2", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover2.jpg", 
                null);
        
        projectService.createProject(input1, testUser.getId());
        projectService.createProject(input2, testUser.getId());

        // When
        List<Project> projects = projectService.getAllProjectsByUserId(testUser.getId());

        // Then
        assertEquals(2, projects.size());
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals("Project 1")));
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals("Project 2")));
    }

    @Test
    @DisplayName("Gallery persistence across database operations")
    void testGalleryPersistence() {
        // Given
        List<String> gallery = new ArrayList<>();
        gallery.add("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image1.jpg");
        gallery.add("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image2.jpg");

        ProjectInput input = new ProjectInput("Persistence Test", 
                "https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/cover.jpg", 
                gallery);
        Project project = projectService.createProject(input, testUser.getId());
        Long projectId = project.getId();

        // When - Fetch from database
        Project fetchedProject = projectService.getProjectById(projectId, testUser.getId());

        // Then - Gallery should be persisted
        assertEquals(2, fetchedProject.getGallery().size());
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image1.jpg", 
                fetchedProject.getGallery().get(0));
        assertEquals("https://res.cloudinary.com/dfuh1mdzq/image/upload/v123/image2.jpg", 
                fetchedProject.getGallery().get(1));
    }
}

