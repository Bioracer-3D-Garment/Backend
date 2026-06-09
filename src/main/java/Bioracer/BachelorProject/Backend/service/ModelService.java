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
        if (modelInput.gender().toString().isBlank()) {
            throw new ModelException("Gender is required.");
        }
        if (modelRepository.existsByName(modelInput.name())) {
            throw new ModelException("Model with name: " + modelInput.name() + " already exists.");
        }
        return modelRepository.save(new Model(modelInput.name(),
                sanitizeFilename(modelInput.coverImage()),
                sanitizeFilename(modelInput.front()),
                sanitizeFilename(modelInput.back()),
                sanitizeFilename(modelInput.side()),
                modelInput.gender()));
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
            model.setCoverImage(sanitizeFilename(modelInput.coverImage()));
        }
        model.setName(modelInput.name());
        model.setFront(sanitizeFilename(modelInput.front()));
        model.setBack(sanitizeFilename(modelInput.back()));
        model.setSide(sanitizeFilename(modelInput.side()));
        model.setGender(modelInput.gender());
        return modelRepository.save(model);
    }

    private String sanitizeFilename(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.contains("://")) {
            try {
                java.net.URI uri = new java.net.URI(trimmed);
                trimmed = uri.getPath();
            } catch (Exception e) {
                // ignore and fall back to string extraction
            }
        }
        int slashIndex = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        return slashIndex >= 0 ? trimmed.substring(slashIndex + 1) : trimmed;
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
