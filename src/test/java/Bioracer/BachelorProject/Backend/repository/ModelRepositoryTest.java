package Bioracer.BachelorProject.Backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import Bioracer.BachelorProject.Backend.model.Gender;
import Bioracer.BachelorProject.Backend.model.Model;

@RepositoryTest
class ModelRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ModelRepository modelRepository;

    private Model gaelle;

    @BeforeEach
    void setUp() {
        gaelle = entityManager.persist(
                new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE));
        entityManager.flush();
    }

    @Test
    void savedModelCanBeFoundById() {
        assertThat(modelRepository.findById(gaelle.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getName()).isEqualTo("Gaëlle");
                    assertThat(found.getCoverImage()).isEqualTo("cover.jpg");
                    assertThat(found.getFront()).isEqualTo("front.jpg");
                    assertThat(found.getBack()).isEqualTo("back.jpg");
                    assertThat(found.getSide()).isEqualTo("side.jpg");
                    assertThat(found.getGender()).isEqualTo(Gender.FEMALE);
                });
    }

    @Test
    void existsByNameReturnsTrueForExistingName() {
        assertThat(modelRepository.existsByName("Gaëlle")).isTrue();
    }

    @Test
    void existsByNameReturnsFalseForUnknownName() {
        assertThat(modelRepository.existsByName("Unknown")).isFalse();
    }

    @Test
    void saveAssignsGeneratedId() {
        Model saved = modelRepository.save(
                new Model("Other", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.MALE));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void deleteRemovesModel() {
        modelRepository.delete(gaelle);
        entityManager.flush();

        assertThat(modelRepository.findById(gaelle.getId())).isEmpty();
    }

    @Test
    void existsByNameIsCaseSensitive() {
        // documents that the duplicate-name check matches exactly on casing
        assertThat(modelRepository.existsByName("gaëlle")).isFalse();
    }

    @Test
    void persistingModelWithBlankNameFails() {
        assertThatThrownBy(() -> {
            entityManager.persist(new Model("", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void persistingModelWithNullGenderFails() {
        assertThatThrownBy(() -> {
            entityManager.persist(new Model("Other", "front.jpg", "back.jpg", "side.jpg", null));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }
}
