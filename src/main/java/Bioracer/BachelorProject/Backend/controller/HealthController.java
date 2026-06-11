package Bioracer.BachelorProject.Backend.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import Bioracer.BachelorProject.Backend.pipeline.adapters.KlingAdapter;

@RestController
@RequestMapping("/health")
public class HealthController {
    private final KlingAdapter klingAdapter;

    public HealthController(KlingAdapter klingAdapter) {
        this.klingAdapter = klingAdapter;
    }

    @GetMapping()
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

}
