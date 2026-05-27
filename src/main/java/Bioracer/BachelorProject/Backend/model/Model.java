package Bioracer.BachelorProject.Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Front image is required.")
    private String front;

    @NotBlank(message = "Back image is required.")
    private String back;

    @NotBlank(message = "Side image is required.")
    private String side;

    @NotNull(message = "Gender is required.")
    private Gender gender;

    protected Model() {
    }

    public Model(String Name, String front, String back, String side,
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

}
