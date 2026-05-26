package Bioracer.BachelorProject.Backend.repository;

import Bioracer.BachelorProject.Backend.model.GeneratedAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedAssetRepository extends JpaRepository<GeneratedAsset, Long> {

    List<GeneratedAsset> findByJobId(String jobId);

    List<GeneratedAsset> findAllByProject_Id(Long projectId);

    Page<GeneratedAsset> findByProject_Id(Long projectId, Pageable pageable);

    Page<GeneratedAsset> findByProject_IdAndCategory(Long projectId, String category, Pageable pageable);

    Page<GeneratedAsset> findByProject_IdAndJobId(Long projectId, String jobId, Pageable pageable);

    Page<GeneratedAsset> findByProject_IdAndJobIdAndCategory(Long projectId, String jobId, String category, Pageable pageable);
}
