package Bioracer.BachelorProject.Backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Bioracer.BachelorProject.Backend.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<Bioracer.BachelorProject.Backend.model.User> findByEmail(String email);

    boolean existsByEmail(String email);
}
