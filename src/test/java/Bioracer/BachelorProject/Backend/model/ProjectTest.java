package Bioracer.BachelorProject.Backend.model;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final User user = new User("Thomas", "Bosmans", "thomas@example.com", "validpassword", Role.USER);

    @BeforeAll
    public static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    public static void tearDown() {
        validatorFactory.close();
    }

    private boolean hasViolation(Project project, String property, String message) {
        Set<ConstraintViolation<Project>> violations = validator.validate(project);
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(property)
                        && v.getMessage().equals(message));
    }

    @Test
    public void GivenValidProject_WhenCreatingProjectWithNameAndUser_ThenProjectIsCreated(){
        Project project = new Project("Summer Collection", user);

        assertEquals("Summer Collection", project.getName());
        assertEquals(user, project.getUser());
        assertNull(project.getCoverImage());
        assertNull(project.getImages());
    }

    @Test
    public void GivenValidProject_WhenCreatingProjectWithCoverImage_ThenProjectIsCreated(){
        Project project = new Project("Summer Collection", user, "cover.jpg");

        assertEquals("Summer Collection", project.getName());
        assertEquals(user, project.getUser());
        assertEquals("cover.jpg", project.getCoverImage());
        assertNull(project.getImages());
    }

    @Test
    public void GivenValidProject_WhenCreatingProjectWithImages_ThenProjectIsCreated(){
        List<String> images = List.of("image1.jpg", "image2.jpg");
        Project project = new Project("Summer Collection", user, "cover.jpg", images);

        assertEquals("Summer Collection", project.getName());
        assertEquals(user, project.getUser());
        assertEquals("cover.jpg", project.getCoverImage());
        assertEquals(images, project.getImages());
    }

    @Test
    public void GivenEmptyName_WhenCreatingProject_ThenErrorIsThrown(){
        Project project = new Project("", user);

        assertTrue(hasViolation(project, "name", "Project name is required."));
    }

    @Test
    public void GivenNullName_WhenCreatingProject_ThenErrorIsThrown(){
        Project project = new Project(null, user);

        assertTrue(hasViolation(project, "name", "Project name is required."));
    }

    @Test
    public void GivenNullUser_WhenCreatingProject_ThenErrorIsThrown(){
        Project project = new Project("Summer Collection", null);

        assertTrue(hasViolation(project, "user", "User is required."));
    }
}
