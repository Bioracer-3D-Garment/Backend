package Bioracer.BachelorProject.Backend.repository;

import org.springframework.stereotype.Component;

import Bioracer.BachelorProject.Backend.model.Gender;
import Bioracer.BachelorProject.Backend.model.Model;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DbInitializer {
        private UserRepository userRepository;
        private ProjectRepository projectRepository;
        private PasswordEncoder passwordEncoder;
        private ModelRepository modelRepository;

        public DbInitializer(UserRepository userRepository, ProjectRepository projectRepository,
                        PasswordEncoder passwordEncoder, ModelRepository modelRepository) {
                this.userRepository = userRepository;
                this.projectRepository = projectRepository;
                this.passwordEncoder = passwordEncoder;
                this.modelRepository = modelRepository;
        }

        public void ClearAll() {
                userRepository.deleteAll();
                projectRepository.deleteAll();
                modelRepository.deleteAll();
        }

        @PostConstruct
        public void initialize() {
                ClearAll();

                User admin = new User("admin", "admin", "admin@bioracer.be", passwordEncoder.encode("admin123"),
                                Role.ADMIN);

                userRepository.save(admin);

                Project project = new Project("Test Project", admin,
                                "fashn-export-1778657803428_ebqsrs",
                                new ArrayList<>(List.of("fashn-export-1778657803428_ebqsrs", "rood_weenmr")));
                projectRepository.save(project);

                Model gaelle = new Model("Gaelle", "model_1_coverImage", "model_1_front", "model_1_back",
                                "model_1_side",
                                Gender.FEMALE);
                Model patrick = new Model("Patrick", "model_2_coverImage", "model_2_front", "model_2_back",
                                "model_2_side",
                                Gender.MALE);
                modelRepository.save(gaelle);
                modelRepository.save(patrick);
        }
}
