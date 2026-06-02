package Bioracer.BachelorProject.Backend.pipeline.models;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "batch_job")
public class AssetGenerationJob {

    @Id
    private String jobId;

    private String runId;

    @Enumerated(EnumType.STRING)
    private AssetGenerationStatus status;

    private int totalCount;

    private int completedCount;

    private int uploadedCount;

    private LocalDateTime createdAt;

    private Long folderId;

    @Column(columnDefinition = "TEXT")
    private String outputPath;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Convert(converter = FailedItemListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<FailedItem> failedItems = new ArrayList<>();

    protected AssetGenerationJob() {}

    public AssetGenerationJob(String jobId, String runId, String outputPath, int totalCount, Long folderId) {
        this.jobId = jobId;
        this.runId = runId;
        this.outputPath = outputPath;
        this.totalCount = totalCount;
        this.folderId = folderId;
        this.status = AssetGenerationStatus.PENDING;
        this.completedCount = 0;
        this.createdAt = LocalDateTime.now();
        this.failedItems = new ArrayList<>();
    }

    public String getJobId() { return jobId; }

    public String getRunId() { return runId; }

    public AssetGenerationStatus getStatus() { return status; }
    public void setStatus(AssetGenerationStatus status) { this.status = status; }

    public int getTotalCount() { return totalCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public int getUploadedCount() { return uploadedCount; }
    public void setUploadedCount(int uploadedCount) { this.uploadedCount = uploadedCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public Long getFolderId() { return folderId; }

    public String getOutputPath() { return outputPath; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<FailedItem> getFailedItems() { return failedItems; }
    public void setFailedItems(List<FailedItem> failedItems) { this.failedItems = failedItems; }
}
