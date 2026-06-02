package Bioracer.BachelorProject.Backend.pipeline.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import Bioracer.BachelorProject.Backend.pipeline.models.AssetGenerationJob;

public interface AssetGenerationJobRepository extends JpaRepository<AssetGenerationJob, String> {}
