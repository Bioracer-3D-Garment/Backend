package Bioracer.BachelorProject.Backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import Bioracer.BachelorProject.Backend.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByUserId(long id);
}
