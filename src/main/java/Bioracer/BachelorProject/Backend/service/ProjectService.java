package Bioracer.BachelorProject.Backend.service;

import Bioracer.BachelorProject.Backend.controller.DTO.AuthenticationResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.controller.DTO.UserInput;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.repository.UserRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        if (project.getUser().getId() != userId) {
            throw new UserException("Not authorized!");
        }
        return project;

    }

    public Project createProject(ProjectInput projectInput, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User does not exist."));
        return projectRepository.save(new Project(projectInput.name(), user, null));
    }

    public Project updateProjectDetails(long id, ProjectInput projectInput){

        var existingProject = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project with ID: " + id + " not found."));

        if (projectInput.name() != null && projectInput.name() != existingProject.getName()) {
            existingProject.setName(projectInput.name());
        }
        if (projectInput.coverImage() != null && projectInput.coverImage() != existingProject.getCoverImage()) {
            existingProject.setCoverImage(projectInput.coverImage());
        }

        return projectRepository.save(existingProject);

        } 
}
