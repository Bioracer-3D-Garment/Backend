package Bioracer.BachelorProject.Backend.service;

import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ProjectService {
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public ProjectService(UserRepository userRepository,
            ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
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
        return projectRepository.save(new Project(projectInput.name(), user, projectInput.coverImage(), projectInput.images()));
    }

    public Project updateProjectDetails(long id, ProjectInput projectInput, Long userId) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project with ID: " + id + " not found."));
        if (!existingProject.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized!");
        }
        if (!projectInput.name().equals(existingProject.getName())) {
            existingProject.setName(projectInput.name());
        }
        if (!Objects.equals(projectInput.coverImage(), existingProject.getCoverImage())) {
            existingProject.setCoverImage(projectInput.coverImage());
        }
        if (!Objects.equals(projectInput.images(), existingProject.getImages())) {
            existingProject.setImages(projectInput.images());
        }

        return projectRepository.save(existingProject);
    }

    public void deleteProject(Long id, Long userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Project with ID: " + id + " not found."));

        if (!project.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized!");
        }

        projectRepository.delete(project);
    }
}
