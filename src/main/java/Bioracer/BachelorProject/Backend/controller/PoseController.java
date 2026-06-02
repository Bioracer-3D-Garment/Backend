package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.pipeline.services.AssetGenerationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/poses")
public class PoseController {

    @Value("${pipeline.poses-dir:}")
    private String posesDir;

    /**
     * GET /api/poses
     * Lists all subdirectories of pipeline.poses-dir as selectable poses.
     * Each subdirectory name is the stable ID used in POST /api/batches poseIds[].
     */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> listPoses() throws IOException {
        if (posesDir.isBlank()) return ResponseEntity.ok(List.of());

        Path dir = Paths.get(posesDir);
        if (!Files.isDirectory(dir)) return ResponseEntity.ok(List.of());

        List<Map<String, String>> poses = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isDirectory).forEach(subdir -> {
                String id = subdir.getFileName().toString();
                poses.add(Map.of(
                        "id",           id,
                        "label",        capitalize(id),
                        "thumbnailUrl", "/poses/" + id + "/thumbnail"
                ));
            });
        }

        return ResponseEntity.ok(poses);
    }

    /**
     * GET /api/poses/{id}/thumbnail
     * Serves the first image file found in {pipeline.poses-dir}/{id}/.
     */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String id) throws IOException {
        if (posesDir.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Poses directory not configured");
        }

        Path poseDir = Paths.get(posesDir, id);
        if (!Files.isDirectory(poseDir)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pose not found: " + id);
        }

        // Walk into top/ or bottom/ subdirectories to find the first image
        Path imageFile;
        try (Stream<Path> stream = Files.walk(poseDir)) {
            imageFile = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> AssetGenerationService.isImageFile(p.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No image found for pose: " + id));
        }

        byte[] bytes = Files.readAllBytes(imageFile);
        return ResponseEntity.ok()
                .contentType(detectMediaType(imageFile.getFileName().toString()))
                .body(bytes);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static MediaType detectMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
