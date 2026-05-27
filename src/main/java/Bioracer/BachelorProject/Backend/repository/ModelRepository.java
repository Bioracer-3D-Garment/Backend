package Bioracer.BachelorProject.Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import Bioracer.BachelorProject.Backend.model.Model;

public interface ModelRepository extends JpaRepository<Model, Long> {
    boolean existsByName(String name);
}
