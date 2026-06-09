package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping("/model/poses")
    public ResponseEntity<UploadService.UploadResult> upload(
            @RequestParam MultipartFile file, @RequestParam String modelId, @RequestParam String pose)
            throws IOException {

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Uploaded file must have a filename");
        }
        UploadService.UploadResult result = uploadService.upload(file.getBytes(), filename);
        return ResponseEntity.ok(result);
    }
}
