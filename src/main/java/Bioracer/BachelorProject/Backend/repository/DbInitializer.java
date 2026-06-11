package Bioracer.BachelorProject.Backend.repository;

import org.springframework.stereotype.Component;

import Bioracer.BachelorProject.Backend.model.Gender;
import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import Bioracer.BachelorProject.Backend.model.Model;
import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
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
        @Profile("local")
        public void initialize() {
                ClearAll();

                User admin = new User("admin", "admin", "admin@bioracer.be", passwordEncoder.encode("admin123"),
                                Role.ADMIN);

                userRepository.save(admin);

                Project project = new Project("Demo Project", admin,
                                "bioracer-logo.png");
                projectRepository.save(project);

                GeneratedAsset asset1 = new GeneratedAsset(project,
                                "https://python-file-server-91ix.onrender.com/files/model_1_front.jpg",
                                "https://python-file-server-91ix.onrender.com/files/model_1_front.jpg",
                                "model_1_front.jpg");

                GeneratedAsset asset2 = new GeneratedAsset(project,
                                "https://python-file-server-91ix.onrender.com/files/model_1_back.jpg",
                                "https://python-file-server-91ix.onrender.com/files/model_1_back.jpg",
                                "model_1_back.jpg");

                GeneratedAsset asset3 = new GeneratedAsset(project,
                                "https://python-file-server-91ix.onrender.com/files/model_1_side.jpg",
                                "https://python-file-server-91ix.onrender.com/files/model_1_side.jpg",
                                "model_1_side.jpg");
                assetRepository.save(asset1);
                assetRepository.save(asset2);
                assetRepository.save(asset3);

                Model model1 = new Model("Model 1", "model_1_coverImage.jpg", "model_1_front.jpg", "model_1_back.jpg",
                                "model_1_side.jpg",
                                Gender.FEMALE);
                Model model2 = new Model("Model 2", "model_2_coverImage.jpg", "model_2_front.jpg", "model_2_back.jpg",
                                "model_2_side.jpg",
                                Gender.MALE);
                Model model3 = new Model("Model 3", "model_3_coverImage.jpg", "model_3_front.jpg", "model_3_back.jpg",
                                "model_3_side.jpg",
                                Gender.FEMALE);
                Model model4 = new Model("Model 4", "model_4_coverImage.jpg", "model_4_front.jpg", "model_4_back.jpg",
                                "model_4_side.jpg", Gender.MALE);
                modelRepository.save(model1);
                modelRepository.save(model2);
                modelRepository.save(model3);
                modelRepository.save(model4);
        }
}
