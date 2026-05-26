package Bioracer.BachelorProject.Backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "project")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name is required.")
    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @NotNull(message = "User is required.")
    private User user;

    private String coverImage;

    @ElementCollection
    @CollectionTable(name = "project_gallery", joinColumns = @JoinColumn(name = "project_id"))
    private List<String> gallery = new ArrayList<>();

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public List<String> getGallery() {
        return gallery;
    }

    public void setGallery(List<String> gallery) {
        this.gallery = gallery != null ? gallery : new ArrayList<>();
    }

    protected Project() {
    }

    public Project(String name, User user, String coverImage) {
        setName(name);
        setUser(user);
        setCoverImage(coverImage);
        this.gallery = new ArrayList<>();
    }

    public Project(String name, User user, String coverImage, List<String> gallery) {
        setName(name);
        setUser(user);
        setCoverImage(coverImage);
        setGallery(gallery);
    }

    public Project(String name, User user, String coverImage, List<String> images) {
        setName(name);
        setUser(user);
        setCoverImage(coverImage);
        setImages(images);
    }

    public Project(String name, User user) {
        setName(name);
        setUser(user);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((coverImage == null) ? 0 : coverImage.hashCode());
        result = prime * result + ((gallery == null) ? 0 : gallery.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Project other = (Project) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (coverImage == null) {
            if (other.coverImage != null)
                return false;
        } else if (!coverImage.equals(other.coverImage))
            return false;
        if (gallery == null) {
            if (other.gallery != null)
                return false;
        } else if (!gallery.equals(other.gallery))
            return false;
        return true;
    }

}
