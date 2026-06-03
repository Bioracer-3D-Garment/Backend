package Bioracer.BachelorProject.Backend.repository;

import org.springframework.stereotype.Component;

import Bioracer.BachelorProject.Backend.model.Gender;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
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
@Profile("local")
public class DbInitializer {
        private UserRepository userRepository;
        private ProjectRepository projectRepository;
        private PasswordEncoder passwordEncoder;
        private ModelRepository modelRepository;
        private GeneratedAssetRepository assetRepository;

        public DbInitializer(UserRepository userRepository, ProjectRepository projectRepository,
                        PasswordEncoder passwordEncoder, ModelRepository modelRepository,
                        GeneratedAssetRepository assetRepository) {
                this.userRepository = userRepository;
                this.projectRepository = projectRepository;
                this.passwordEncoder = passwordEncoder;
                this.modelRepository = modelRepository;
                this.assetRepository = assetRepository;
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
                                "Stella_Artois_logo.svg.png");
                projectRepository.save(project);

                GeneratedAsset asset1 = new GeneratedAsset(project, "http://localhost:8080/model_1_front.png",
                                "http://localhost:8080/model_1_front.png", "model_1_front.png");
                assetRepository.save(asset1);

                Model gaelle = new Model("Gaelle", "model_1_coverImage.png", "model_1_front.png", "model_1_back.png",
                                "model_1_side.png",
                                Gender.FEMALE);
                Model patrick = new Model("Patrick", "model_2_coverImage.png", "model_2_front.png", "model_2_back.png",
                                "model_2_side.png",
                                Gender.MALE);
                modelRepository.save(gaelle);
                modelRepository.save(patrick);
        }
}
