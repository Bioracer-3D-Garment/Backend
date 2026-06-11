package Bioracer.BachelorProject.Backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolationException;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;

@RepositoryTest
class ProjectRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepository;

    private User jane;
    private User john;

    @BeforeEach
    void setUp() {
        jane = entityManager.persist(new User("Jane", "Doe", "jane@example.com", "hashed", Role.USER));
        john = entityManager.persist(new User("John", "Doe", "john@example.com", "hashed", Role.USER));
        entityManager.flush();
    }

    @Test
    void findAllByUserIdReturnsOnlyProjectsOfThatUser() {
        Project janesProject = entityManager.persist(new Project("Jane's Project", jane));
        entityManager.persist(new Project("John's Project", john));
        entityManager.flush();

        List<Project> projects = projectRepository.findAllByUserId(jane.getId());

        assertThat(projects).hasSize(1);
        assertThat(projects.get(0).getId()).isEqualTo(janesProject.getId());
        assertThat(projects.get(0).getName()).isEqualTo("Jane's Project");
    }

    @Test
    void findAllByUserIdReturnsEmptyListWhenUserHasNoProjects() {
        assertThat(projectRepository.findAllByUserId(jane.getId())).isEmpty();
    }

    @Test
    void galleryImagesArePersistedInOrder() {
        Project project = entityManager.persist(
                new Project("Project", jane, "cover.jpg", List.of("first.jpg", "second.jpg", "third.jpg")));
        entityManager.flush();
        entityManager.clear();

        Project reloaded = projectRepository.findById(project.getId()).orElseThrow();

        assertThat(reloaded.getImages()).containsExactly("first.jpg", "second.jpg", "third.jpg");
    }

    @Test
    void findAllByUserIdReturnsAllProjectsOfThatUser() {
        entityManager.persist(new Project("First", jane));
        entityManager.persist(new Project("Second", jane));
        entityManager.flush();

        assertThat(projectRepository.findAllByUserId(jane.getId()))
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("First", "Second");
    }

    @Test
    void updatingImagesReplacesGallery() {
        Project project = entityManager.persist(
                new Project("Project", jane, "cover.jpg", List.of("old.jpg")));
        entityManager.flush();

        project.setImages(List.of("new-1.jpg", "new-2.jpg"));
        projectRepository.saveAndFlush(project);
        entityManager.clear();

        Project reloaded = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(reloaded.getImages()).containsExactly("new-1.jpg", "new-2.jpg");
    }

    @Test
    void deletingProjectDoesNotDeleteUser() {
        Project project = entityManager.persist(new Project("Project", jane));
        entityManager.flush();

        projectRepository.delete(project);
        entityManager.flush();

        assertThat(projectRepository.findById(project.getId())).isEmpty();
        assertThat(entityManager.find(User.class, jane.getId())).isNotNull();
    }

    @Test
    void persistingProjectWithoutUserFails() {
        assertThatThrownBy(() -> {
            entityManager.persist(new Project("Project", null));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void persistingProjectWithBlankNameFails() {
        assertThatThrownBy(() -> {
            entityManager.persist(new Project("", jane));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }
}
