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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    public void GivenValidModelWithCoverImage_WhenCreatingModel_ThenModelIsCreated(){
        Model gaelle = new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertEquals("Gaëlle", gaelle.getName());
        assertEquals("cover.jpg", gaelle.getCoverImage());
        assertEquals("front.jpg", gaelle.getFront());
        assertEquals("back.jpg", gaelle.getBack());
        assertEquals("side.jpg", gaelle.getSide());
        assertEquals(Gender.FEMALE, gaelle.getGender());
    }

    @Test
    public void GivenValidModel_WhenValidating_ThenThereAreNoViolations(){
        Model model = new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertTrue(validator.validate(model).isEmpty());
    }

    @Test
    public void GivenWhitespaceName_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("   ", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "name", "Name is required."));
    }

    @Test
    public void GivenWhitespaceFront_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "   ", "back.jpg", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "front", "Front is required."));
    }

    @Test
    public void GivenWhitespaceBack_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "front.jpg", "   ", "side.jpg", Gender.FEMALE);

        assertTrue(hasViolation(model, "back", "Back is required."));
    }

    @Test
    public void GivenWhitespaceSide_WhenCreatingModel_ThenErrorIsThrown(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", "   ", Gender.FEMALE);

        assertTrue(hasViolation(model, "side", "Side is required."));
    }

    @Test
    public void GivenModel_WhenUpdatingWithSetters_ThenValuesAreUpdated(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        model.setName("Updated");
        model.setCoverImage("new-cover.jpg");
        model.setFront("new-front.jpg");
        model.setBack("new-back.jpg");
        model.setSide("new-side.jpg");
        model.setGender(Gender.MALE);

        assertEquals("Updated", model.getName());
        assertEquals("new-cover.jpg", model.getCoverImage());
        assertEquals("new-front.jpg", model.getFront());
        assertEquals("new-back.jpg", model.getBack());
        assertEquals("new-side.jpg", model.getSide());
        assertEquals(Gender.MALE, model.getGender());
    }

    @Test
    public void GivenTwoModelsWithSameValues_WhenComparing_ThenTheyAreEqualWithSameHashCode(){
        Model first = new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void GivenModel_WhenComparingToItself_ThenItIsEqual(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertEquals(model, model);
    }

    @Test
    public void GivenModel_WhenComparingToNull_ThenItIsNotEqual(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertFalse(model.equals(null));
    }

    @Test
    public void GivenModel_WhenComparingToDifferentType_ThenItIsNotEqual(){
        Model model = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertFalse(model.equals("Gaëlle"));
    }

    @Test
    public void GivenTwoModelsWithDifferentName_WhenComparing_ThenTheyAreNotEqual(){
        Model first = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Other", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoModelsWithDifferentCoverImage_WhenComparing_ThenTheyAreNotEqual(){
        Model first = new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "other.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoModelsWithDifferentFront_WhenComparing_ThenTheyAreNotEqual(){
        Model first = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "other.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoModelsWithDifferentBack_WhenComparing_ThenTheyAreNotEqual(){
        Model first = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "front.jpg", "other.jpg", "side.jpg", Gender.FEMALE);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoModelsWithDifferentSide_WhenComparing_ThenTheyAreNotEqual(){
        Model first = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "front.jpg", "back.jpg", "other.jpg", Gender.FEMALE);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenTwoModelsWithDifferentGender_WhenComparing_ThenTheyAreNotEqual(){
        Model first = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.MALE);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenModelWithNullCoverImage_WhenComparingToModelWithCoverImage_ThenTheyAreNotEqual(){
        Model first = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertNotEquals(first, second);
    }

    @Test
    public void GivenModelWithNullName_WhenComparingToModelWithName_ThenTheyAreNotEqual(){
        Model first = new Model(null, "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        Model second = new Model("Gaëlle", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertNotEquals(first, second);
    }
}
