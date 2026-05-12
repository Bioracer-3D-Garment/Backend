package Bioracer.BachelorProject.Backend.repository;

import org.springframework.stereotype.Component;

import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import jakarta.annotation.PostConstruct;

import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DbInitializer {
        private UserRepository userRepository;
        private PasswordEncoder passwordEncoder;

        public DbInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
        }

        public void ClearAll() {
                userRepository.deleteAll();
        }

        @PostConstruct
        public void initialize() {
                ClearAll();

                User admin = new User("admin", "admin", "admin@bioracer.be", passwordEncoder.encode("admin123"),
                                Role.ADMIN);

                userRepository.save(admin);

        }

}
