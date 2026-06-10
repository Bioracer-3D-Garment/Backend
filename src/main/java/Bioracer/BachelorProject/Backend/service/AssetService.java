package Bioracer.BachelorProject.Backend.service;

import Bioracer.BachelorProject.Backend.controller.DTO.GeneratedAssetResponse;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.repository.GeneratedAssetRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AssetService {

    private final GeneratedAssetRepository assetRepository;
    private final ProjectRepository projectRepository;
    private final UploadService uploadService;

    public AssetService(GeneratedAssetRepository assetRepository,
            ProjectRepository projectRepository,
            UploadService uploadService) {
        this.assetRepository = assetRepository;
        this.projectRepository = projectRepository;
        this.uploadService = uploadService;
    }

    public record ProjectAssetsPage(Long projectId, long totalCount, int page, int size,
            List<GeneratedAssetResponse> assets) {
    }

    public ProjectAssetsPage getProjectAssets(Long projectId, Long userId,
            String jobId, String category,
            int page, int size) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        if (!project.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        int effectiveSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, effectiveSize);

        Page<GeneratedAsset> result;
        if (jobId != null && category != null) {
            result = assetRepository.findByProject_IdAndJobIdAndCategory(projectId, jobId, category, pageable);
        } else if (jobId != null) {
            result = assetRepository.findByProject_IdAndJobId(projectId, jobId, pageable);
        } else if (category != null) {
            result = assetRepository.findByProject_IdAndCategory(projectId, category, pageable);
        } else {
            result = assetRepository.findByProject_Id(projectId, pageable);
        }

        List<GeneratedAssetResponse> assets = result.getContent().stream()
                .map(GeneratedAssetResponse::from)
                .toList();

        return new ProjectAssetsPage(projectId, result.getTotalElements(), page, effectiveSize, assets);
    }

    public void deleteAsset(Long assetId, Long userId) {
        GeneratedAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Asset not found: " + assetId));
        if (!asset.getProject().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        uploadService.delete(asset.getPublicId());
        assetRepository.delete(asset);
    }

    public List<GeneratedAsset> getProjectAssetsForDownload(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        if (!project.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return assetRepository.findAllByProject_Id(projectId);
    }
}
