package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.util.ReflectionTestUtils;

import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("Jane", "Doe", "jane@example.com", "hashed", Role.USER);
        ReflectionTestUtils.setField(owner, "id", 1000L);
    }

    @Test
    void getAllProjectsReturnsAllProjects() {
        List<Project> projects = List.of(
                new Project("Project One", owner),
                new Project("Project Two", owner));
        when(projectRepository.findAll()).thenReturn(projects);

        assertThat(projectService.getAllProjects()).containsExactlyElementsOf(projects);
    }

    @Test
    void getAllProjectsByUserIdReturnsProjectsOfUser() {
        List<Project> projects = List.of(new Project("Project One", owner));
        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.findAllByUserId(1000L)).thenReturn(projects);

        assertThat(projectService.getAllProjectsByUserId(1000L)).containsExactlyElementsOf(projects);
    }

    @Test
    void getAllProjectsByUserIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getAllProjectsByUserId(9999L))
                .isInstanceOf(UserException.class)
                .hasMessage("User does not exist!");
    }

    @Test
    void createProjectStoresCoverAndGalleryImages() {
        ProjectInput input = new ProjectInput(
                "Race Dashboard",
                "cover.jpg",
                List.of("gallery-1.jpg", "gallery-2.jpg"));

        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.createProject(input, 1000L);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());

        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getName()).isEqualTo("Race Dashboard");
        assertThat(savedProject.getUser()).isEqualTo(owner);
        assertThat(savedProject.getCoverImage()).isEqualTo("cover.jpg");
        assertThat(savedProject.getImages()).containsExactly("gallery-1.jpg", "gallery-2.jpg");
        assertThat(created.getImages()).containsExactly("gallery-1.jpg", "gallery-2.jpg");
    }

    @Test
    void createProjectStripsPathsAndUrlsToFilenames() {
        ProjectInput input = new ProjectInput(
                "Race Dashboard",
                "https://example.com/uploads/v1/cover.jpg",
                List.of("uploads\\gallery-1.jpg", "/var/images/gallery-2.jpg"));

        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.createProject(input, 1000L);

        assertThat(created.getCoverImage()).isEqualTo("cover.jpg");
        assertThat(created.getImages()).containsExactly("gallery-1.jpg", "gallery-2.jpg");
    }

    @Test
    void createProjectThrowsWhenUserDoesNotExist() {
        ProjectInput input = new ProjectInput("Race Dashboard", "cover.jpg", List.of("gallery-1.jpg"));

        when(userRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(input, 9999L))
                .isInstanceOf(UserException.class)
                .hasMessage("User does not exist.");
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void getProjectByIdReturnsProjectForOwner() {
        Project existing = new Project("Project", owner, "cover.jpg", List.of("gallery.jpg"));
        ReflectionTestUtils.setField(existing, "id", 77L);

        when(projectRepository.findById(77L)).thenReturn(Optional.of(existing));

        Project found = projectService.getProjectById(77L, 1000L);

        assertThat(found.getId()).isEqualTo(77L);
        assertThat(found.getImages()).containsExactly("gallery.jpg");
    }

    @Test
    void getProjectByIdThrowsWhenProjectDoesNotExist() {
        when(projectRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(77L, 1000L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Project with ID: 77 not found.");
    }

    @Test
    void getProjectByIdThrowsWhenRequesterDoesNotOwnProject() {
        Project existing = new Project("Project", owner);
        ReflectionTestUtils.setField(existing, "id", 77L);

        when(projectRepository.findById(77L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> projectService.getProjectById(77L, 2000L))
                .isInstanceOf(UserException.class)
                .hasMessage("Not authorized!");
    }

    @Test
    void updateProjectDetailsUpdatesCoverAndGalleryImages() {
        Project existing = new Project("Old Name", owner, "old-cover.jpg", List.of("old-gallery.jpg"));
        ReflectionTestUtils.setField(existing, "id", 12L);

        ProjectInput input = new ProjectInput(
                "New Name",
                "new-cover.jpg",
                List.of("new-gallery.jpg"));

        when(projectRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(existing)).thenReturn(existing);

        Project updated = projectService.updateProjectDetails(12L, input, 1000L);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getCoverImage()).isEqualTo("new-cover.jpg");
        assertThat(updated.getImages()).containsExactly("new-gallery.jpg");
    }

    @Test
    void updateProjectDetailsThrowsWhenProjectDoesNotExist() {
        ProjectInput input = new ProjectInput("New Name", "new-cover.jpg", List.of("new-gallery.jpg"));

        when(projectRepository.findById(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProjectDetails(12L, input, 1000L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project with ID: 12 not found.");
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void updateProjectDetailsThrowsWhenRequesterDoesNotOwnProject() {
        Project existing = new Project("Project", owner);
        ReflectionTestUtils.setField(existing, "id", 12L);

        ProjectInput input = new ProjectInput("New Name", "new-cover.jpg", List.of("new-gallery.jpg"));

        when(projectRepository.findById(12L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> projectService.updateProjectDetails(12L, input, 2000L))
                .isInstanceOf(UserException.class)
                .hasMessage("Not authorized!");
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void deleteProjectDeletesProjectForOwner() {
        Project existing = new Project("Project", owner);
        ReflectionTestUtils.setField(existing, "id", 55L);

        when(projectRepository.findById(55L)).thenReturn(Optional.of(existing));

        projectService.deleteProject(55L, 1000L);

        verify(projectRepository).delete(existing);
    }

    @Test
    void deleteProjectThrowsWhenProjectDoesNotExist() {
        when(projectRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(55L, 1000L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Project with ID: 55 not found.");
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    void deleteProjectThrowsWhenRequesterDoesNotOwnProject() {
        Project existing = new Project("Project", owner);
        ReflectionTestUtils.setField(existing, "id", 55L);

        when(projectRepository.findById(55L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> projectService.deleteProject(55L, 2000L))
                .isInstanceOf(UserException.class)
                .hasMessage("Not authorized!");
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    void getAllProjectsReturnsEmptyListWhenThereAreNoProjects() {
        when(projectRepository.findAll()).thenReturn(List.of());

        assertThat(projectService.getAllProjects()).isEmpty();
    }

    @Test
    void getAllProjectsByUserIdReturnsEmptyListWhenUserHasNoProjects() {
        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.findAllByUserId(1000L)).thenReturn(List.of());

        assertThat(projectService.getAllProjectsByUserId(1000L)).isEmpty();
    }

    @Test
    void createProjectKeepsNullCoverAndImages() {
        ProjectInput input = new ProjectInput("Race Dashboard", null, null);

        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.createProject(input, 1000L);

        assertThat(created.getCoverImage()).isNull();
        assertThat(created.getImages()).isNull();
    }

    @Test
    void createProjectStoresEmptyImagesList() {
        ProjectInput input = new ProjectInput("Race Dashboard", "cover.jpg", List.of());

        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.createProject(input, 1000L);

        assertThat(created.getImages()).isEmpty();
    }

    @Test
    void updateProjectDetailsKeepsValuesWhenInputIsUnchanged() {
        Project existing = new Project("Same Name", owner, "cover.jpg", List.of("gallery.jpg"));
        ReflectionTestUtils.setField(existing, "id", 12L);

        ProjectInput input = new ProjectInput("Same Name", "cover.jpg", List.of("gallery.jpg"));

        when(projectRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(existing)).thenReturn(existing);

        Project updated = projectService.updateProjectDetails(12L, input, 1000L);

        assertThat(updated.getName()).isEqualTo("Same Name");
        assertThat(updated.getCoverImage()).isEqualTo("cover.jpg");
        assertThat(updated.getImages()).containsExactly("gallery.jpg");
        verify(projectRepository).save(existing);
    }

    @Test
    void createProjectTrimsWhitespaceFromFilenames() {
        ProjectInput input = new ProjectInput("Race Dashboard", "  cover.jpg  ", List.of("  gallery.jpg  "));

        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.createProject(input, 1000L);

        assertThat(created.getCoverImage()).isEqualTo("cover.jpg");
        assertThat(created.getImages()).containsExactly("gallery.jpg");
    }

    @Test
    void createProjectDropsQueryStringWhenSanitizingUrls() {
        ProjectInput input = new ProjectInput("Race Dashboard",
                "https://example.com/files/cover.jpg?version=2&size=large", List.of());

        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.createProject(input, 1000L);

        assertThat(created.getCoverImage()).isEqualTo("cover.jpg");
    }

    @Test
    void createProjectFallsBackToStringExtractionForMalformedUrls() {
        // "http://[invalid" cannot be parsed as a URI, so the filename is cut after the last slash
        ProjectInput input = new ProjectInput("Race Dashboard", "http://[invalid/uploads/cover.jpg", List.of());

        when(userRepository.findById(1000L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.createProject(input, 1000L);

        assertThat(created.getCoverImage()).isEqualTo("cover.jpg");
    }
}
