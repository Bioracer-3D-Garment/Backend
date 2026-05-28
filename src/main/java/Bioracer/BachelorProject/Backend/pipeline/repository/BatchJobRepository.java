package Bioracer.BachelorProject.Backend.pipeline.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import Bioracer.BachelorProject.Backend.pipeline.models.BatchJob;

public interface BatchJobRepository extends JpaRepository<BatchJob, String> {}
