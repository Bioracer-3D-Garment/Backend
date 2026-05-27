package Bioracer.BachelorProject.Backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import Bioracer.BachelorProject.Backend.controller.DTO.ModelInput;
import Bioracer.BachelorProject.Backend.exception.ModelException;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.model.Model;
import Bioracer.BachelorProject.Backend.repository.ModelRepository;

@Service
public class ModelService {
    private final ModelRepository modelRepository;

    public ModelService(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    public Model getModelById(long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Model with ID: " + id + " not found."));
        return model;
    }

    public Model createModel(ModelInput modelInput) {
        if (modelInput.name().isBlank()) {
            throw new ModelException("Name is required.");
        }
        if (modelInput.front().isBlank()) {
            throw new ModelException("Front image is required.");
        }
        if (modelInput.back().isBlank()) {
            throw new ModelException("Back image is required.");
        }
        if (modelInput.side().isBlank()) {
            throw new ModelException("Side image is required.");
        }
        if (modelInput.gender().toString().isBlank()) {
            throw new ModelException("Gender is required.");
        }
        if (modelRepository.existsByName(modelInput.name())) {
            throw new ModelException("Model with name: " + modelInput.name() + " already exists.");
        }
        return modelRepository.save(new Model(modelInput.name(), modelInput.front(), modelInput.back(),
                modelInput.side(), modelInput.gender()));
    }

    public Model updateModelDetails(ModelInput modelInput, long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Model with ID: " + id + " does not exist."));
        if (modelInput.name().isBlank()) {
            throw new ModelException("Name is required.");
        }
        if (modelInput.front().isBlank()) {
            throw new ModelException("Front image is required.");
        }
        if (modelInput.back().isBlank()) {
            throw new ModelException("Back image is required.");
        }
        if (modelInput.side().isBlank()) {
            throw new ModelException("Side image is required.");
        }
        if (modelInput.gender().toString().isBlank()) {
            throw new ModelException("Gender is required.");
        }
        if (!modelInput.coverImage().isBlank()) {
            model.setCoverImage(modelInput.coverImage());
        }
        model.setName(modelInput.name());
        model.setFront(modelInput.front());
        model.setBack(modelInput.back());
        model.setSide(modelInput.side());
        model.setGender(modelInput.gender());
        return modelRepository.save(model);
    }

    public String deleteModel(long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Model with ID: " + id + " does not exist."));

        try {
            modelRepository.delete(model);
        } catch (Exception e) {
            throw new ModelException("Delete failed!");
        }
        return "Succesfully Deleted";
    }
}
