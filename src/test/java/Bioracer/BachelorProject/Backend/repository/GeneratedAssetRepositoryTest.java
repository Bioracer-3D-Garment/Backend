package Bioracer.BachelorProject.Backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;

@RepositoryTest
class GeneratedAssetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GeneratedAssetRepository assetRepository;

    private Project project;
    private Project otherProject;
    private GeneratedAsset jobOneFrontAsset;
    private GeneratedAsset jobOneBackAsset;
    private GeneratedAsset jobTwoFrontAsset;

    @BeforeEach
    void setUp() {
        User jane = entityManager.persist(new User("Jane", "Doe", "jane@example.com", "hashed", Role.USER));
        project = entityManager.persist(new Project("Project", jane));
        otherProject = entityManager.persist(new Project("Other Project", jane));

        jobOneFrontAsset = entityManager.persist(new GeneratedAsset(project, "job-1", "product-1", "pose-1",
                "front", "secure-1.jpg", "thumb-1.jpg", "public-1"));
        jobOneBackAsset = entityManager.persist(new GeneratedAsset(project, "job-1", "product-1", "pose-2",
                "back", "secure-2.jpg", "thumb-2.jpg", "public-2"));
        jobTwoFrontAsset = entityManager.persist(new GeneratedAsset(project, "job-2", "product-2", "pose-1",
                "front", "secure-3.jpg", "thumb-3.jpg", "public-3"));
        entityManager.persist(new GeneratedAsset(otherProject, "job-3", "product-3", "pose-1",
                "front", "secure-4.jpg", "thumb-4.jpg", "public-4"));
        entityManager.flush();
    }

    @Test
    void findByJobIdReturnsAssetsOfThatJob() {
        List<GeneratedAsset> assets = assetRepository.findByJobId("job-1");

        assertThat(assets).extracting(GeneratedAsset::getId)
                .containsExactlyInAnyOrder(jobOneFrontAsset.getId(), jobOneBackAsset.getId());
    }

    @Test
    void findByJobIdReturnsEmptyListForUnknownJob() {
        assertThat(assetRepository.findByJobId("unknown-job")).isEmpty();
    }

    @Test
    void findAllByProjectIdReturnsOnlyAssetsOfThatProject() {
        List<GeneratedAsset> assets = assetRepository.findAllByProject_Id(project.getId());

        assertThat(assets).extracting(GeneratedAsset::getId)
                .containsExactlyInAnyOrder(jobOneFrontAsset.getId(), jobOneBackAsset.getId(),
                        jobTwoFrontAsset.getId());
    }

    @Test
    void findByProjectIdReturnsPagedAssets() {
        Page<GeneratedAsset> page = assetRepository.findByProject_Id(project.getId(), PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void findByProjectIdAndCategoryFiltersByCategory() {
        Page<GeneratedAsset> page = assetRepository.findByProject_IdAndCategory(project.getId(), "front",
                PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(GeneratedAsset::getId)
                .containsExactlyInAnyOrder(jobOneFrontAsset.getId(), jobTwoFrontAsset.getId());
    }

    @Test
    void findByProjectIdAndJobIdFiltersByJobId() {
        Page<GeneratedAsset> page = assetRepository.findByProject_IdAndJobId(project.getId(), "job-1",
                PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(GeneratedAsset::getId)
                .containsExactlyInAnyOrder(jobOneFrontAsset.getId(), jobOneBackAsset.getId());
    }

    @Test
    void findByProjectIdAndJobIdAndCategoryFiltersByBoth() {
        Page<GeneratedAsset> page = assetRepository.findByProject_IdAndJobIdAndCategory(project.getId(),
                "job-1", "front", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(GeneratedAsset::getId)
                .containsExactly(jobOneFrontAsset.getId());
    }

    @Test
    void saveAssignsGeneratedIdAndPersistsCreatedAt() {
        GeneratedAsset saved = assetRepository
                .saveAndFlush(new GeneratedAsset(project, "secure-5.jpg", "thumb-5.jpg", "public-5"));
        entityManager.clear();

        GeneratedAsset reloaded = assetRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void secondPageContainsRemainingAssets() {
        Page<GeneratedAsset> page = assetRepository.findByProject_Id(project.getId(), PageRequest.of(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByProjectIdReturnsEmptyPageForUnknownProject() {
        Page<GeneratedAsset> page = assetRepository.findByProject_Id(9999L, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findByProjectIdAndJobIdReturnsEmptyPageForUnknownJob() {
        Page<GeneratedAsset> page = assetRepository.findByProject_IdAndJobId(project.getId(), "unknown-job",
                PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findByProjectIdAndCategoryReturnsEmptyPageForUnknownCategory() {
        Page<GeneratedAsset> page = assetRepository.findByProject_IdAndCategory(project.getId(),
                "unknown-category", PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void deletingAssetDoesNotDeleteProjectOrOtherAssets() {
        assetRepository.delete(jobOneFrontAsset);
        entityManager.flush();

        assertThat(entityManager.find(Project.class, project.getId())).isNotNull();
        assertThat(assetRepository.findAllByProject_Id(project.getId()))
                .extracting(GeneratedAsset::getId)
                .containsExactlyInAnyOrder(jobOneBackAsset.getId(), jobTwoFrontAsset.getId());
    }
}
