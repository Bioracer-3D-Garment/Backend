package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
public class UploadController {

    private final CloudinaryService cloudinaryService;

    public UploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<CloudinaryService.UploadResult> upload(
            @RequestParam MultipartFile file) throws IOException {

        String publicId = "bioracer/uploads/" + UUID.randomUUID();
        CloudinaryService.UploadResult result = cloudinaryService.upload(file.getBytes(), publicId);
        return ResponseEntity.ok(result);
    }
}
