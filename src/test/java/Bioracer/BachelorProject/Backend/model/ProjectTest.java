package Bioracer.BachelorProject.Backend.model;

import java.util.ArrayList;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    public void GivenValidProject_WhenValidating_ThenThereAreNoViolations(){
        Project project = new Project("Summer Collection", user, "cover.jpg", List.of("image1.jpg"));

        assertTrue(validator.validate(project).isEmpty());
    }

    @Test
    public void GivenWhitespaceName_WhenCreatingProject_ThenErrorIsThrown(){
        Project project = new Project("   ", user);

        assertTrue(hasViolation(project, "name", "Project name is required."));
    }

    @Test
    public void GivenProject_WhenUpdatingWithSetters_ThenValuesAreUpdated(){
        Project project = new Project("Summer Collection", user);
        User otherUser = new User("John", "Doe", "john@example.com", "validpassword", Role.ADMIN);

        project.setName("Winter Collection");
        project.setUser(otherUser);
        project.setCoverImage("new-cover.jpg");
        project.setImages(List.of("new-image.jpg"));

        assertEquals("Winter Collection", project.getName());
        assertEquals(otherUser, project.getUser());
        assertEquals("new-cover.jpg", project.getCoverImage());
        assertEquals(List.of("new-image.jpg"), project.getImages());
    }

    @Test
    public void GivenProject_WhenSettingImages_ThenAMutatedSourceListDoesNotChangeTheProject(){
        Project project = new Project("Summer Collection", user);
        List<String> source = new ArrayList<>(List.of("image1.jpg"));

        project.setImages(source);
        source.add("image2.jpg");

        assertEquals(List.of("image1.jpg"), project.getImages());
    }

    @Test
    public void GivenProject_WhenSettingImagesToNull_ThenImagesAreNull(){
        Project project = new Project("Summer Collection", user, "cover.jpg", List.of("image1.jpg"));

        project.setImages(null);

        assertNull(project.getImages());
    }

    @Test
    public void GivenTwoProjectsWithSameValues_WhenComparing_ThenTheyAreEqualWithSameHashCode(){
        Project first = new Project("Summer Collection", user, "cover.jpg", List.of("image1.jpg"));
        Project second = new Project("Summer Collection", user, "cover.jpg", List.of("image1.jpg"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void GivenProject_WhenComparingToItself_ThenItIsEqual(){
        Project project = new Project("Summer Collection", user);

        assertEquals(project, project);
    }

    @Test
    public void GivenProject_WhenComparingToNull_ThenItIsNotEqual(){
        Project project = new Project("Summer Collection", user);

        assertFalse(project.equals(null));
    }

    @Test
    public void GivenProject_WhenComparingToDifferentType_ThenItIsNotEqual(){
        Project project = new Project("Summer Collection", user);

        assertFalse(project.equals("Summer Collection"));
    }

    @Test
    public void GivenTwoProjectsWithDifferentName_WhenComparing_ThenTheyAreNotEqual(){
        Project first = new Project("Summer Collection", user);
        Project second = new Project("Winter Collection", user);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoProjectsWithDifferentUser_WhenComparing_ThenTheyAreNotEqual(){
        User otherUser = new User("John", "Doe", "john@example.com", "validpassword", Role.USER);
        Project first = new Project("Summer Collection", user);
        Project second = new Project("Summer Collection", otherUser);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoProjectsWithDifferentCoverImage_WhenComparing_ThenTheyAreNotEqual(){
        Project first = new Project("Summer Collection", user, "cover.jpg");
        Project second = new Project("Summer Collection", user, "other.jpg");

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoProjectsWithDifferentImages_WhenComparing_ThenTheyAreNotEqual(){
        Project first = new Project("Summer Collection", user, "cover.jpg", List.of("image1.jpg"));
        Project second = new Project("Summer Collection", user, "cover.jpg", List.of("image2.jpg"));

        assertNotEquals(first, second);
    }

    @Test
    public void GivenProjectWithNullCoverImage_WhenComparingToProjectWithCoverImage_ThenTheyAreNotEqual(){
        Project first = new Project("Summer Collection", user);
        Project second = new Project("Summer Collection", user, "cover.jpg");

        assertNotEquals(first, second);
    }

    @Test
    public void GivenProjectWithNullImages_WhenComparingToProjectWithImages_ThenTheyAreNotEqual(){
        Project first = new Project("Summer Collection", user, "cover.jpg");
        Project second = new Project("Summer Collection", user, "cover.jpg", List.of("image1.jpg"));

        assertNotEquals(first, second);
    }
}
