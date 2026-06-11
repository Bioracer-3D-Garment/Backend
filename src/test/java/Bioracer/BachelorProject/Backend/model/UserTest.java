package Bioracer.BachelorProject.Backend.model;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    public static void tearDown() {
        validatorFactory.close();
    }

    private boolean hasViolation(User user, String property, String message) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(property)
                        && v.getMessage().equals(message));
    }

    @Test
    public void GivenValidUser_WhenCreatingUser_ThenUserIsCreated(){
        User thomas = new User("Thomas", "Bosmans", "thomas@example.com", "validpassword", Role.ADMIN);

        assertEquals("Thomas", thomas.getFirstName());
        assertEquals("Bosmans", thomas.getLastName());
        assertEquals("thomas@example.com", thomas.getEmail());
        assertEquals("validpassword", thomas.getPassword());
        assertEquals(Role.ADMIN, thomas.getRole());
    }

    @Test
    public void GivenEmptyFirstName_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("", "Bosmans", "thomas@example.com", "validpassword", Role.ADMIN);

        assertTrue(hasViolation(user, "firstName", "First name is required."));
    }

    @Test
    public void GivenNullFirstName_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User(null, "Bosmans", "thomas@example.com", "validpassword", Role.ADMIN);

        assertTrue(hasViolation(user, "firstName", "First name is required."));
    }

    @Test
    public void GivenEmptyLastName_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("Thomas", "", "thomas@example.com", "validpassword", Role.ADMIN);

        assertTrue(hasViolation(user, "lastName", "Last name is required."));
    }

    @Test
    public void GivenNullLastName_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("Thomas", null, "thomas@example.com", "validpassword", Role.ADMIN);

        assertTrue(hasViolation(user, "lastName", "Last name is required."));
    }

    @Test
    public void GivenEmptyEmail_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("Thomas", "Bosmans", "", "validpassword", Role.ADMIN);

        assertTrue(hasViolation(user, "email", "Email is required."));
    }

    @Test
    public void GivenNullEmail_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("Thomas", "Bosmans", null, "validpassword", Role.ADMIN);

        assertTrue(hasViolation(user, "email", "Email is required."));
    }

    @Test
    public void GivenEmptyPassword_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("Thomas", "Bosmans", "thomas@example.com", "", Role.ADMIN);

        assertTrue(hasViolation(user, "password", "Password is required."));
    }

    @Test
    public void GivenNullPassword_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("Thomas", "Bosmans", "thomas@example.com", null, Role.ADMIN);

        assertTrue(hasViolation(user, "password", "Password is required."));
    }

    @Test
    public void GivenNullRole_WhenCreatingUser_ThenErrorIsThrown(){
        User user = new User("Thomas", "Bosmans", "thomas@example.com", "validpassword", null);

        assertTrue(hasViolation(user, "role", "Role is required."));
    }
}
