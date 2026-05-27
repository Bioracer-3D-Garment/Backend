package Bioracer.BachelorProject.Backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_asset")
public class GeneratedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    private String jobId;

    private String productId;

    private String poseId;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String secureUrl;

    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String publicId;

    private LocalDateTime createdAt;

    protected GeneratedAsset() {}

    public GeneratedAsset(Project project, String jobId, String productId, String poseId,
                          String category, String secureUrl, String thumbnailUrl, String publicId) {
        this.project = project;
        this.jobId = jobId;
        this.productId = productId;
        this.poseId = poseId;
        this.category = category;
        this.secureUrl = secureUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.publicId = publicId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getJobId() { return jobId; }
    public String getProductId() { return productId; }
    public String getPoseId() { return poseId; }
    public String getCategory() { return category; }
    public String getSecureUrl() { return secureUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getPublicId() { return publicId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
