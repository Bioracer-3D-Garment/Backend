package Bioracer.BachelorProject.Backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Bioracer.BachelorProject.Backend.controller.DTO.ModelInput;
import Bioracer.BachelorProject.Backend.model.Model;
import Bioracer.BachelorProject.Backend.service.ModelService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/model")
public class ModelController {
    private ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    // only users and admin
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping()
    public List<Model> getAll() {
        return modelService.getAllModels();
    }

    // only users and admin
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/id")
    public Model getById(@RequestParam Long id) {
        return modelService.getModelById(id);
    }

    // only users and admin
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping()
    public Model createModel(@Valid @RequestBody ModelInput modelInput) {
        return modelService.createModel(modelInput);
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PutMapping("{id}")
    public Model updateModelDetails(@PathVariable Long id, @RequestBody ModelInput modelInput) {
        return modelService.updateModelDetails(modelInput, id);
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteModel(
            @PathVariable Long id) {
        modelService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }

}
