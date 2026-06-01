package Bioracer.BachelorProject.Backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.service.JwtService;
import Bioracer.BachelorProject.Backend.service.ProjectService;
import Bioracer.BachelorProject.Backend.util.TestDataFactory;

/**
 * Unit tests for {@link ProjectController}.
 *
 * Uses standalone MockMvc with mocked collaborators, so no Spring context is started.
 * The tests focus on request mapping, Authorization-header handling and delegation to
 * the service. Service-level authorization rules are covered in ProjectServiceTest.
 */
class ProjectControllerTest {

    private static final String TOKEN = "test-token";
    private static final String AUTH_HEADER = "Bearer " + TOKEN;
    private static final Long USER_ID = 1000L;

    private final ProjectService projectService = mock(ProjectService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProjectController controller = new ProjectController(projectService, jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ExceptionHandlers())
                .build();
    }

    @Test
    void getUserProjectsReturnsProjectsForTokenOwner() throws Exception {
        User owner = TestDataFactory.user("owner@example.com");
        Project project = TestDataFactory.project("Race Dashboard", owner);

        when(jwtService.extractId(TOKEN)).thenReturn(USER_ID);
        when(projectService.getAllProjectsByUserId(USER_ID)).thenReturn(List.of(project));

        mockMvc.perform(get("/projects/user").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Race Dashboard"))
                .andExpect(jsonPath("$[0].coverImage").value(TestDataFactory.COVER_URL));
    }

    @Test
    void getByIdReturnsProject() throws Exception {
        User owner = TestDataFactory.user("owner@example.com");
        Project project = TestDataFactory.project("Race Dashboard", owner);

        when(jwtService.extractId(TOKEN)).thenReturn(USER_ID);
        when(projectService.getProjectById(42L, USER_ID)).thenReturn(project);

        mockMvc.perform(get("/projects/id").param("id", "42").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Race Dashboard"));
    }

    @Test
    void createProjectDelegatesToServiceWithTokenUserId() throws Exception {
        User owner = TestDataFactory.user("owner@example.com");
        Project saved = TestDataFactory.project("New Project", owner);
        ProjectInput input = TestDataFactory.projectInput("New Project");

        when(jwtService.extractId(TOKEN)).thenReturn(USER_ID);
        when(projectService.createProject(any(ProjectInput.class), eq(USER_ID))).thenReturn(saved);

        mockMvc.perform(post("/projects")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Project"));

        verify(projectService).createProject(any(ProjectInput.class), eq(USER_ID));
    }

    @Test
    void updateProjectReturnsUpdatedProject() throws Exception {
        User owner = TestDataFactory.user("owner@example.com");
        Project updated = TestDataFactory.project("Updated Name", owner);
        ProjectInput input = TestDataFactory.projectInput("Updated Name");

        when(jwtService.extractId(TOKEN)).thenReturn(USER_ID);
        when(projectService.updateProjectDetails(eq(7L), any(ProjectInput.class), eq(USER_ID)))
                .thenReturn(updated);

        mockMvc.perform(put("/projects/7")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void deleteProjectReturnsNoContent() throws Exception {
        when(jwtService.extractId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(delete("/projects/9").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(9L, USER_ID);
    }

    @Test
    void requestWithMalformedAuthHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/projects/user").header("Authorization", "NotBearerToken"))
                .andExpect(status().isUnauthorized());
    }
}
