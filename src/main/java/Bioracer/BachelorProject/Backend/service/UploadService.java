package Bioracer.BachelorProject.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Service
public class UploadService {

    private final WebClient webClient;
    private final String uploadServerUrl;

    public UploadService(@Value("${upload.server.url:http://localhost:8081}") String uploadServerUrl) {
        this.uploadServerUrl = uploadServerUrl.replaceAll("/+$", "");
        this.webClient = WebClient.builder()
                .baseUrl(this.uploadServerUrl)
                .build();
    }

    public record UploadResult(String secureUrl, String publicId, String thumbnailUrl, String filename) {
    }

    private record UploadResponse(String filename, String url) {
    }

    public UploadResult upload(byte[] imageBytes, String filename) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename is required for upload");
        }
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        UploadResponse response = webClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(UploadResponse.class)
                .block();

        if (response == null || response.filename() == null || response.url() == null) {
            throw new IllegalStateException("Upload server returned an invalid response");
        }
        ;
        return new UploadResult(response.url(), response.filename(), response.url(), response.filename());
    }

    /**
     * Upload a video file to the upload server.
     * 
     * @param videoBytes  raw video bytes
     * @param filename    desired filename (required)
     * @param contentType MIME type (e.g. "video/mp4"); if null defaults to
     *                    "video/mp4"
     * @return UploadResult with URL and filename
     */
    public UploadResult uploadVideo(byte[] videoBytes, String filename) {
        if (videoBytes == null || videoBytes.length == 0) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename is required for upload");
        }
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        String effectiveContentType = "video/mp4";
        builder.part("file", new ByteArrayResource(videoBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).filename(filename).contentType(MediaType.parseMediaType(effectiveContentType));

        UploadResponse response = webClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(UploadResponse.class)
                .block();

        if (response == null || response.filename() == null || response.url() == null) {
            throw new IllegalStateException("Upload server returned an invalid response");
        }
        return new UploadResult(response.url(), response.filename(), response.url(), response.filename());
    }

    /**
     * Download a video by its file reference (URL or server reference). Delegates
     * to {@link #download(String)}.
     */
    public byte[] downloadVideo(String fileReference) {
        return download(fileReference);
    }

    /**
     * Delete a video by its file reference (URL or server reference). Delegates to
     * {@link #delete(String)}.
     */
    public void deleteVideo(String fileReference) {
        delete(fileReference);
    }

    public byte[] download(String fileReference) {
        if (fileReference == null || fileReference.isBlank()) {
            throw new IllegalArgumentException("Cannot download file: reference is blank");
        }
        try {
            String url = uploadServerUrl + "/" + fileReference;
            System.out.println("Downloading file from URL: " + url);
            byte[] bytes = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("File server returned empty body for reference=" + fileReference);
            }
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("File download failed for reference=" + fileReference, e);
        }
    }

    public void delete(String fileReference) {
        if (fileReference == null || fileReference.isBlank()) {
            throw new IllegalArgumentException("Cannot delete file: reference is blank");
        }
        try {
            String url = uploadServerUrl + "/" + fileReference;
            webClient.delete()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("File delete failed for reference=" + fileReference, e);
        }
    }
}
