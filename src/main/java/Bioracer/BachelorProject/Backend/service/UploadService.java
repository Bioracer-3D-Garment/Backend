package Bioracer.BachelorProject.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Service
public class UploadService {

    private final WebClient webClient;
    private final String uploadServerUrl;

    public UploadService(@Value("${upload.server.url}") String uploadServerUrl) {
        this.uploadServerUrl = uploadServerUrl.replaceAll("/+$", "");

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(50 * 1024 * 1024))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(this.uploadServerUrl)
                .exchangeStrategies(strategies)
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

        ByteArrayResource resource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        UploadResponse response = webClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", resource))
                .retrieve()
                .bodyToMono(UploadResponse.class)
                .block();

        if (response == null || response.filename() == null || response.url() == null) {
            throw new IllegalStateException("Upload server returned an invalid response");
        }
        return new UploadResult(
                response.url(),
                response.filename(),
                response.url(),
                response.filename());
    }

    public UploadResult uploadVideo(byte[] videoBytes, String filename) {
        if (videoBytes == null || videoBytes.length == 0) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename is required for upload");
        }
        ByteArrayResource resource = new ByteArrayResource(videoBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("file", resource)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.formData()
                                .name("file")
                                .filename(filename + ".mp4") // ensures Flask sees it in request.files
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("video/mp4"));

        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        UploadResponse response = webClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBody))
                .retrieve()
                .bodyToMono(UploadResponse.class)
                .block();

        if (response == null || response.filename() == null || response.url() == null) {
            throw new IllegalStateException("Upload server returned an invalid response");
        }

        return new UploadResult(
                response.url(),
                response.filename(),
                response.url(),
                response.filename());
    }

    public byte[] downloadVideo(String fileReference) {
        return download(fileReference);
    }

    public void deleteVideo(String fileReference) {
        delete(fileReference);
    }

    public byte[] download(String fileReference) {
        if (fileReference == null || fileReference.isBlank()) {
            throw new IllegalArgumentException("Cannot download file: reference is blank");
        }

        try {
            String url = uploadServerUrl + "/files/" + fileReference;
            System.out.println(url);
            return webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("File download failed for reference=" + fileReference, e);
        }
    }

    public void delete(String fileReference) {
        if (fileReference == null || fileReference.isBlank()) {
            throw new IllegalArgumentException("Cannot delete file: reference is blank");
        }

        try {
            String url = uploadServerUrl + "/files/" + fileReference;

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