package Bioracer.BachelorProject.Backend.service;

import Bioracer.BachelorProject.Backend.controller.DTO.AssetInput;
import Bioracer.BachelorProject.Backend.controller.DTO.GenerateInput;
import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.model.Asset;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;
import Bioracer.BachelorProject.Backend.pipeline.services.BatchService;
import Bioracer.BachelorProject.Backend.pipeline.utils.CatalogProduct;
import Bioracer.BachelorProject.Backend.repository.AssetRepository;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

@Service
public class ProjectService {
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AssetRepository assetRepository;
    private final BatchService batchService;

    public ProjectService(UserRepository userRepository,
            ProjectRepository projectRepository,
            AssetRepository assetRepository,
            BatchService batchService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.assetRepository = assetRepository;
        this.batchService = batchService;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public List<Project> getAllProjectsByUserId(Long id) {
        userRepository.findById(id).orElseThrow(() -> new UserException("User does not exist!"));
        return projectRepository.findAllByUserId(id);
    }

    public Project getProjectById(Long id, Long userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project with ID: " + id + " not found."));
        if (!project.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized!");
        }
        return project;

    }

    public Project createProject(ProjectInput projectInput, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User does not exist."));
        return projectRepository.save(new Project(projectInput.name(), user, projectInput.coverImage(), projectInput.gallery()));
    }

    public Project updateProjectDetails(long id, ProjectInput projectInput, Long userId){
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project with ID: " + id + " not found."));

        if (!existingProject.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized!");
        }

        if (projectInput.name() != null && !projectInput.name().equals(existingProject.getName())) {
            existingProject.setName(projectInput.name());
        }
        if (projectInput.coverImage() != null && !projectInput.coverImage().equals(existingProject.getCoverImage())) {
            existingProject.setCoverImage(projectInput.coverImage());
        }
        if (projectInput.gallery() != null && !projectInput.gallery().equals(existingProject.getGallery())) {
            existingProject.setGallery(projectInput.gallery());
        }

        return projectRepository.save(existingProject);
    }

    public String generateGallery(Long projectId, GenerateInput input, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project with ID: " + projectId + " not found."));
        if (!project.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized!");
        }

        byte[] garmentBytes = RestClient.create().get()
                .uri(input.garmentUrl())
                .retrieve()
                .body(byte[].class);
        if (garmentBytes == null || garmentBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not download garment image from URL");
        }

        String fashnCategory = switch (input.category().toLowerCase()) {
            case "top"    -> "upper_body";
            case "bottom" -> "lower_body";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown category: '" + input.category() + "'. Use 'top' or 'bottom'");
        };

        String normalizedGender = input.gender().trim().toLowerCase();
        List<Path> poses = batchService.resolvePoseFiles(normalizedGender, fashnCategory);
        if (poses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No poses found for gender='" + normalizedGender + "' category='" + input.category() + "'");
        }

        CatalogProduct product = new CatalogProduct("project_" + projectId, fashnCategory, garmentBytes);
        BatchJob job = batchService.createJob(poses.size(), projectId);
        batchService.runBatch(job.getJobId(), List.of(product), normalizedGender);
        return job.getJobId();
    }

    public Asset createAsset(Long projectId, AssetInput input, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project with ID: " + projectId + " not found."));
        if (!project.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized!");
        }
        return assetRepository.save(new Asset(input.name(), input.type(), input.thumbnail(), input.clothing(), input.model(), project));
    }

    public List<Asset> getProjectAssets(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project with ID: " + projectId + " not found."));
        if (!project.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized!");
        }
        return assetRepository.findAllByProjectId(projectId);
    }
}
