package Bioracer.BachelorProject.Backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "model")
public class Model {
    private String coverImage;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Front is required.")
    private String front;

    @NotBlank(message = "Back is required.")
    private String back;

    @NotBlank(message = "Side is required.")
    private String side;

    @NotNull(message = "Gender is required.")
    private Gender gender;

    protected Model() {
    }

    public Model(String name, String front, String back, String side,
            Gender gender) {
        setName(name);
        setFront(front);
        setBack(back);
        setSide(side);
        setGender(gender);
    }

    public Model(String name, String coverImage, String front, String back, String side,
            Gender gender) {
        setName(name);
        setCoverImage(coverImage);
        setFront(front);
        setBack(back);
        setSide(side);
        setGender(gender);
    }

    public Long getId() {
        return id;
    }

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coverImage == null) ? 0 : coverImage.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((front == null) ? 0 : front.hashCode());
        result = prime * result + ((back == null) ? 0 : back.hashCode());
        result = prime * result + ((side == null) ? 0 : side.hashCode());
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());
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
        Model other = (Model) obj;
        if (coverImage == null) {
            if (other.coverImage != null)
                return false;
        } else if (!coverImage.equals(other.coverImage))
            return false;
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
        if (front == null) {
            if (other.front != null)
                return false;
        } else if (!front.equals(other.front))
            return false;
        if (back == null) {
            if (other.back != null)
                return false;
        } else if (!back.equals(other.back))
            return false;
        if (side == null) {
            if (other.side != null)
                return false;
        } else if (!side.equals(other.side))
            return false;
        if (gender != other.gender)
            return false;
        return true;
    }

}
