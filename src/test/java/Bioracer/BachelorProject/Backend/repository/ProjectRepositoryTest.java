package Bioracer.BachelorProject.Backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.util.TestDataFactory;

/**
 * Repository tests for {@link ProjectRepository}, backed by an in-memory H2 database.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepository;

    private User persistUser(String email) {
        return entityManager.persistAndFlush(TestDataFactory.user(email));
    }

    @Test
    void saveAndFindByIdRoundTripsProject() {
        User owner = persistUser("owner@example.com");

        Project saved = projectRepository.save(TestDataFactory.project("Race Dashboard", owner));
        entityManager.flush();
        entityManager.clear();

        Project found = projectRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Race Dashboard");
        assertThat(found.getCoverImage()).isEqualTo(TestDataFactory.COVER_URL);
        assertThat(found.getUser().getId()).isEqualTo(owner.getId());
    }

    @Test
    void findAllByUserIdReturnsOnlyThatUsersProjects() {
        User owner = persistUser("owner@example.com");
        User otherUser = persistUser("other@example.com");

        projectRepository.save(TestDataFactory.project("Owner Project A", owner));
        projectRepository.save(TestDataFactory.project("Owner Project B", owner));
        projectRepository.save(TestDataFactory.project("Other Project", otherUser));
        entityManager.flush();
        entityManager.clear();

        List<Project> ownerProjects = projectRepository.findAllByUserId(owner.getId());

        assertThat(ownerProjects)
                .hasSize(2)
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("Owner Project A", "Owner Project B");
    }

    @Test
    void findAllByUserIdReturnsEmptyWhenUserHasNoProjects() {
        User owner = persistUser("owner@example.com");

        assertThat(projectRepository.findAllByUserId(owner.getId())).isEmpty();
    }

    @Test
    void galleryImagesPersistInOrder() {
        User owner = persistUser("owner@example.com");
        Project project = new Project(
                "Gallery Project",
                owner,
                TestDataFactory.COVER_URL,
                List.of(
                        "https://res.cloudinary.com/demo/image/upload/v1/first.jpg",
                        "https://res.cloudinary.com/demo/image/upload/v1/second.jpg",
                        "https://res.cloudinary.com/demo/image/upload/v1/third.jpg"));

        Project saved = projectRepository.save(project);
        entityManager.flush();
        entityManager.clear();

        Project found = projectRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getImages()).containsExactly(
                "https://res.cloudinary.com/demo/image/upload/v1/first.jpg",
                "https://res.cloudinary.com/demo/image/upload/v1/second.jpg",
                "https://res.cloudinary.com/demo/image/upload/v1/third.jpg");
    }
}
