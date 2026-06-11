package Bioracer.BachelorProject.Backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import Bioracer.BachelorProject.Backend.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<Bioracer.BachelorProject.Backend.model.User> findByEmail(String email);

    boolean existsByEmail(String email);
}
