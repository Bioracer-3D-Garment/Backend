package Bioracer.BachelorProject.Backend.repository;

import org.springframework.stereotype.Component;

import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import jakarta.annotation.PostConstruct;

import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DbInitializer {
        private UserRepository userRepository;
        private ProjectRepository projectRepository;
        private PasswordEncoder passwordEncoder;

        public DbInitializer(UserRepository userRepository, ProjectRepository projectRepository,
                        PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.projectRepository = projectRepository;
                this.passwordEncoder = passwordEncoder;
        }

        public void ClearAll() {
                userRepository.deleteAll();
                projectRepository.deleteAll();
        }

        @PostConstruct
        public void initialize() {
                ClearAll();

                User admin = new User("admin", "admin", "admin@bioracer.be", passwordEncoder.encode("admin123"),
                                Role.ADMIN);

                userRepository.save(admin);

                Project project = new Project("Test Project", admin,
                                "https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885_1280.jpg");
                projectRepository.save(project);

        }

}
