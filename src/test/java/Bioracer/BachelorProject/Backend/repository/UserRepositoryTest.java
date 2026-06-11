package Bioracer.BachelorProject.Backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;

@RepositoryTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User jane;

    @BeforeEach
    void setUp() {
        jane = entityManager.persist(new User("Jane", "Doe", "jane@example.com", "hashed", Role.USER));
        entityManager.flush();
    }

    @Test
    void findByEmailReturnsUser() {
        assertThat(userRepository.findByEmail("jane@example.com"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(jane.getId());
                    assertThat(found.getFirstName()).isEqualTo("Jane");
                    assertThat(found.getRole()).isEqualTo(Role.USER);
                });
    }

    @Test
    void findByEmailReturnsEmptyWhenEmailIsUnknown() {
        assertThat(userRepository.findByEmail("unknown@example.com")).isEmpty();
    }

    @Test
    void existsByEmailReturnsTrueForExistingEmail() {
        assertThat(userRepository.existsByEmail("jane@example.com")).isTrue();
    }

    @Test
    void existsByEmailReturnsFalseForUnknownEmail() {
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    void saveAssignsGeneratedId() {
        User saved = userRepository.save(new User("John", "Doe", "john@example.com", "hashed", Role.USER));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByEmailReturnsCorrectUserAmongMultiple() {
        entityManager.persist(new User("John", "Doe", "john@example.com", "hashed", Role.ADMIN));
        entityManager.flush();

        assertThat(userRepository.findByEmail("john@example.com"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getFirstName()).isEqualTo("John");
                    assertThat(found.getRole()).isEqualTo(Role.ADMIN);
                });
    }

    @Test
    void persistingUserWithBlankEmailFails() {
        assertThatThrownBy(() -> {
            entityManager.persist(new User("John", "Doe", "", "hashed", Role.USER));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void existsByEmailIsCaseSensitive() {
        // documents that email lookups match exactly; logins must use the stored casing
        assertThat(userRepository.existsByEmail("JANE@EXAMPLE.COM")).isFalse();
    }

    @Test
    void persistingUserWithNullRoleFails() {
        assertThatThrownBy(() -> {
            entityManager.persist(new User("John", "Doe", "john@example.com", "hashed", null));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }
}
