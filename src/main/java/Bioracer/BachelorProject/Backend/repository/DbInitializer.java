package Bioracer.BachelorProject.Backend.repository;

import org.springframework.stereotype.Component;

import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.ArrayList;

@Component
@Profile("local")
public class DbInitializer {
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final PasswordEncoder passwordEncoder;


	public DbInitializer(UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder) {
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
				"https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885_1280.jpg",
				new ArrayList<>(List.of(
					"https://res.cloudinary.com/dfuh1mdzq/image/upload/v1234567890/sample1.jpg",
					"https://res.cloudinary.com/dfuh1mdzq/image/upload/v1234567890/sample2.jpg"
				)));
		projectRepository.save(project);

	}

}
