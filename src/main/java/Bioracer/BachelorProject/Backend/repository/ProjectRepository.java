package Bioracer.BachelorProject.Backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Bioracer.BachelorProject.Backend.model.Project;
import Bioracer.BachelorProject.Backend.model.User;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByUserId(long id);
}
