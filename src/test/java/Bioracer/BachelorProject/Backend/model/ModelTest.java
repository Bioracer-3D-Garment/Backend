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

public class ModelTest {
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

    private boolean hasViolation(Model model, String property, String message) {
        Set<ConstraintViolation<Model>> violations = validator.validate(model);
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(property)
                        && v.getMessage().equals(message));
    }

    @Test
    public void GivenValidModel_WhenCreatingModel_ThenModelIsCreated(){
        Model gaelle = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE );

        assertEquals("Gaëlle", gaelle.getName());
        assertEquals("front.jpg", gaelle.getFront());
        assertEquals("back.jpg", gaelle.getBack());
        assertEquals("side.jpg", gaelle.getSide());
        assertEquals(Gender.FEMALE, gaelle.getGender());
    }


    @Test
    public void GivenEmptyName_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "name", "Name is required."));
    }

    @Test
    public void GivenNullName_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model(null, "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "name", "Name is required."));
    }

    @Test
    public void GivenEmptyFront_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "", "back.jpg", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "front", "Front is required."));
    }

    @Test
    public void GivenNullFront_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", null, "back.jpg", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "front", "Front is required."));
    }

    @Test
    public void GivenEmptyBack_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "front.jpg", "", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "back", "Back is required."));
    }

    @Test
    public void GivenNullBack_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "front.jpg", null, "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "back", "Back is required."));
    }

    @Test
    public void GivenEmptySide_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", "", Gender.FEMALE);

        assertTrue(hasViolation(model, "side", "Side is required."));
    }

    @Test
    public void GivenNullSide_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", null, Gender.FEMALE);

        assertTrue(hasViolation(model, "side", "Side is required."));
    }

    @Test
    public void GivenNullGender_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", null);

        assertTrue(hasViolation(model, "gender", "Gender is required."));
    }
}
