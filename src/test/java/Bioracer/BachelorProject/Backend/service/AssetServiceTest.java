package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private GeneratedAssetRepository assetRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UploadService uploadService;

    @InjectMocks
    private AssetService assetService;

    private User owner;
    private Project project;
    private GeneratedAsset asset;

    @BeforeEach
    void setUp() {
        owner = new User("Jane", "Doe", "jane@example.com", "hashed", Role.USER);
        ReflectionTestUtils.setField(owner, "id", 1000L);

        project = new Project("Project", owner);
        ReflectionTestUtils.setField(project, "id", 5L);

        asset = new GeneratedAsset(project, "job-1", "product-1", "pose-1",
                "category-1", "secure.jpg", "thumb.jpg", "public-1");
        ReflectionTestUtils.setField(asset, "id", 42L);
    }

    @Test
    void getProjectAssetsReturnsPageOfAssets() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_Id(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asset), PageRequest.of(0, 20), 1));

        AssetService.ProjectAssetsPage result = assetService.getProjectAssets(5L, 1000L, null, null, 0, 20);

        assertThat(result.projectId()).isEqualTo(5L);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.assets()).hasSize(1);
        assertThat(result.assets().get(0).id()).isEqualTo(42L);
        assertThat(result.assets().get(0).secureUrl()).isEqualTo("secure.jpg");
    }

    @Test
    void getProjectAssetsCapsPageSizeAtHundred() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_Id(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AssetService.ProjectAssetsPage result = assetService.getProjectAssets(5L, 1000L, null, null, 0, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(assetRepository).findByProject_Id(any(Long.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void getProjectAssetsFiltersByJobId() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_IdAndJobId(any(Long.class), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asset)));

        assetService.getProjectAssets(5L, 1000L, "job-1", null, 0, 20);

        verify(assetRepository).findByProject_IdAndJobId(any(Long.class), anyString(), any(Pageable.class));
    }

    @Test
    void getProjectAssetsFiltersByCategory() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_IdAndCategory(any(Long.class), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asset)));

        assetService.getProjectAssets(5L, 1000L, null, "category-1", 0, 20);

        verify(assetRepository).findByProject_IdAndCategory(any(Long.class), anyString(), any(Pageable.class));
    }

    @Test
    void getProjectAssetsFiltersByJobIdAndCategory() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_IdAndJobIdAndCategory(any(Long.class), anyString(), anyString(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asset)));

        assetService.getProjectAssets(5L, 1000L, "job-1", "category-1", 0, 20);

        verify(assetRepository).findByProject_IdAndJobIdAndCategory(any(Long.class), anyString(), anyString(),
                any(Pageable.class));
    }

    @Test
    void getProjectAssetsThrowsWhenProjectDoesNotExist() {
        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getProjectAssets(5L, 1000L, null, null, 0, 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getProjectAssetsThrowsWhenRequesterDoesNotOwnProject() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> assetService.getProjectAssets(5L, 2000L, null, null, 0, 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void deleteAssetDeletesFileAndAsset() {
        when(assetRepository.findById(42L)).thenReturn(Optional.of(asset));

        assetService.deleteAsset(42L, 1000L);

        verify(uploadService).delete("public-1");
        verify(assetRepository).delete(asset);
    }

    @Test
    void deleteAssetThrowsWhenAssetDoesNotExist() {
        when(assetRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.deleteAsset(42L, 1000L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(assetRepository, never()).delete(any(GeneratedAsset.class));
    }

    @Test
    void deleteAssetThrowsWhenRequesterDoesNotOwnAsset() {
        when(assetRepository.findById(42L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.deleteAsset(42L, 2000L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(uploadService, never()).delete(anyString());
        verify(assetRepository, never()).delete(any(GeneratedAsset.class));
    }

    @Test
    void getProjectAssetsForDownloadReturnsAllAssets() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findAllByProject_Id(5L)).thenReturn(List.of(asset));

        assertThat(assetService.getProjectAssetsForDownload(5L, 1000L)).containsExactly(asset);
    }

    @Test
    void getProjectAssetsForDownloadThrowsWhenProjectDoesNotExist() {
        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getProjectAssetsForDownload(5L, 1000L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getProjectAssetsForDownloadThrowsWhenRequesterDoesNotOwnProject() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> assetService.getProjectAssetsForDownload(5L, 2000L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getProjectAssetsReturnsEmptyPageWhenProjectHasNoAssets() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_Id(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AssetService.ProjectAssetsPage result = assetService.getProjectAssets(5L, 1000L, null, null, 0, 20);

        assertThat(result.totalCount()).isZero();
        assertThat(result.assets()).isEmpty();
    }

    @Test
    void getProjectAssetsPassesRequestedPageNumber() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_Id(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AssetService.ProjectAssetsPage result = assetService.getProjectAssets(5L, 1000L, null, null, 2, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(assetRepository).findByProject_Id(any(Long.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(2);
    }

    @Test
    void getProjectAssetsKeepsRequestedSizeWhenBelowCap() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findByProject_Id(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AssetService.ProjectAssetsPage result = assetService.getProjectAssets(5L, 1000L, null, null, 0, 50);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(assetRepository).findByProject_Id(any(Long.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        assertThat(result.size()).isEqualTo(50);
    }

    @Test
    void deleteAssetPropagatesUploadFailureWithoutDeletingAsset() {
        when(assetRepository.findById(42L)).thenReturn(Optional.of(asset));
        doThrow(new RuntimeException("upload server down")).when(uploadService).delete("public-1");

        assertThatThrownBy(() -> assetService.deleteAsset(42L, 1000L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("upload server down");
        verify(assetRepository, never()).delete(any(GeneratedAsset.class));
    }

    @Test
    void getProjectAssetsForDownloadReturnsEmptyListWhenProjectHasNoAssets() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(assetRepository.findAllByProject_Id(5L)).thenReturn(List.of());

        assertThat(assetService.getProjectAssetsForDownload(5L, 1000L)).isEmpty();
    }
}
