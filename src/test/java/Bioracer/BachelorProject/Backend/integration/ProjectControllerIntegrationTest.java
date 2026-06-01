package Bioracer.BachelorProject.Backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import Bioracer.BachelorProject.Backend.controller.DTO.ProjectInput;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.ProjectRepository;
import Bioracer.BachelorProject.Backend.repository.UserRepository;
import Bioracer.BachelorProject.Backend.service.JwtService;
import Bioracer.BachelorProject.Backend.util.TestDataFactory;

/**
 * Full-stack integration tests for the project endpoints: security filter chain,
 * controller, service, JPA and an in-memory H2 database. Each test runs in a
 * transaction that is rolled back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(TestDataFactory.user("owner@example.com"));
        token = jwtService.generateToken(owner);
    }

    @Test
    void createProjectPersistsProjectForAuthenticatedUser() throws Exception {
        ProjectInput input = TestDataFactory.projectInput("Race Dashboard");

        mockMvc.perform(post("/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Race Dashboard"))
                .andExpect(jsonPath("$.coverImage").value(TestDataFactory.COVER_URL));

        List<Project> stored = projectRepository.findAllByUserId(owner.getId());
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getName()).isEqualTo("Race Dashboard");
    }

    @Test
    void getUserProjectsReturnsOnlyOwnedProjects() throws Exception {
        projectRepository.save(TestDataFactory.project("Owned Project", owner));

        User otherUser = userRepository.save(TestDataFactory.user("other@example.com"));
        projectRepository.save(TestDataFactory.project("Other Project", otherUser));

        mockMvc.perform(get("/projects/user").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Owned Project"));
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/projects/user"))
                .andExpect(status().isUnauthorized());
    }
}
