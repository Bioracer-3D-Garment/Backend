package Bioracer.BachelorProject.Backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
}
