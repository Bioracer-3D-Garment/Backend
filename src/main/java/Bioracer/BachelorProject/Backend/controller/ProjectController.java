package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.service.JwtService;
import Bioracer.BachelorProject.Backend.service.ProjectService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final JwtService jwtService;

    public ProjectController(ProjectService projectService, JwtService jwtService) {
        this.projectService = projectService;
        this.jwtService = jwtService;
    }

    // only admin access
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public List<Project> getAll() {
        return projectService.getAllProjects();
    }

    // only users and admin
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/user")
    public List<Project> getAllByUserId(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractIdFromHeader(authHeader);
        return projectService.getAllProjectsByUserId(userId);
    }

    // only users and admin
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/id")
    public Project getById(@RequestParam Long id, @RequestHeader("Authorization") String authHeader) {
        Long userId = extractIdFromHeader(authHeader);
        return projectService.getProjectById(id, userId);
    }

    // only users and admin
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping()
    public Project createProject(@Valid @RequestBody ProjectInput projectInput,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractIdFromHeader(authHeader);
        return projectService.createProject(projectInput, userId);
    }

    private Long extractIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.extractId(token);
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PutMapping("{id}")
    public Project updateProjectDetails(@PathVariable Long id, @Valid @RequestBody ProjectInput projectInput) {
        return projectService.updateProjectDetails(id, projectInput);
    }
}
