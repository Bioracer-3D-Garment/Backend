package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Exercises the real HTTP behavior of UploadService against an embedded JDK
 * HttpServer, so the WebClient request building and response mapping are
 * verified without mocking.
 */
class UploadServiceHttpTest {

    private record ReceivedRequest(String method, String path, String contentType) {
    }

    private HttpServer server;
    private String baseUrl;
    private UploadService uploadService;
    private final List<ReceivedRequest> requests = new CopyOnWriteArrayList<>();

    private volatile String uploadResponseBody;
    private volatile int filesStatus = 200;
    private volatile byte[] fileBytes = new byte[] { 1, 2, 3 };

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/upload", exchange -> {
            requests.add(new ReceivedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Content-Type")));
            exchange.getRequestBody().readAllBytes();
            byte[] body = uploadResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/files", exchange -> {
            requests.add(new ReceivedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Content-Type")));
            exchange.getRequestBody().readAllBytes();
            if (filesStatus != 200) {
                exchange.sendResponseHeaders(filesStatus, -1);
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(200, fileBytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(fileBytes);
                }
            }
            exchange.close();
        });
        server.start();

        baseUrl = "http://localhost:" + server.getAddress().getPort();
        uploadResponseBody = "{\"filename\":\"stored.jpg\",\"url\":\"" + baseUrl + "/files/stored.jpg\"}";
        uploadService = new UploadService(baseUrl);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void uploadReturnsResultMappedFromServerResponse() {
        UploadService.UploadResult result = uploadService.upload(new byte[] { 1 }, "original.jpg");

        assertThat(result.filename()).isEqualTo("stored.jpg");
        assertThat(result.publicId()).isEqualTo("stored.jpg");
        assertThat(result.secureUrl()).isEqualTo(baseUrl + "/files/stored.jpg");
        assertThat(result.thumbnailUrl()).isEqualTo(baseUrl + "/files/stored.jpg");
    }

    @Test
    void uploadSendsMultipartPostToUploadEndpoint() {
        uploadService.upload(new byte[] { 1 }, "original.jpg");

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).method()).isEqualTo("POST");
        assertThat(requests.get(0).path()).isEqualTo("/upload");
        assertThat(requests.get(0).contentType()).startsWith("multipart/form-data");
    }

    @Test
    void uploadThrowsWhenServerResponseOmitsFilename() {
        uploadResponseBody = "{\"url\":\"" + baseUrl + "/files/stored.jpg\"}";

        assertThatThrownBy(() -> uploadService.upload(new byte[] { 1 }, "original.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Upload server returned an invalid response");
    }

    @Test
    void uploadThrowsWhenServerResponseOmitsUrl() {
        uploadResponseBody = "{\"filename\":\"stored.jpg\"}";

        assertThatThrownBy(() -> uploadService.upload(new byte[] { 1 }, "original.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Upload server returned an invalid response");
    }

    @Test
    void uploadVideoReturnsResultMappedFromServerResponse() {
        UploadService.UploadResult result = uploadService.uploadVideo(new byte[] { 1 }, "clip");

        assertThat(result.filename()).isEqualTo("stored.jpg");
        assertThat(result.secureUrl()).isEqualTo(baseUrl + "/files/stored.jpg");
        assertThat(requests.get(0).method()).isEqualTo("POST");
        assertThat(requests.get(0).path()).isEqualTo("/upload");
    }

    @Test
    void downloadReturnsFileBytesFromFilesEndpoint() {
        byte[] downloaded = uploadService.download("stored.jpg");

        assertThat(downloaded).containsExactly(1, 2, 3);
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).method()).isEqualTo("GET");
        assertThat(requests.get(0).path()).isEqualTo("/files/stored.jpg");
    }

    @Test
    void downloadVideoReturnsFileBytes() {
        assertThat(uploadService.downloadVideo("stored.jpg")).containsExactly(1, 2, 3);
    }

    @Test
    void downloadWrapsServerErrors() {
        filesStatus = 500;

        assertThatThrownBy(() -> uploadService.download("missing.jpg"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File download failed for reference=missing.jpg");
    }

    @Test
    void deleteSendsDeleteToFilesEndpoint() {
        uploadService.delete("stored.jpg");

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).method()).isEqualTo("DELETE");
        assertThat(requests.get(0).path()).isEqualTo("/files/stored.jpg");
    }

    @Test
    void deleteWrapsServerErrors() {
        filesStatus = 500;

        assertThatThrownBy(() -> uploadService.delete("stored.jpg"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File delete failed for reference=stored.jpg");
    }

    @Test
    void trailingSlashesInServerUrlAreTrimmed() {
        UploadService serviceWithSlashes = new UploadService(baseUrl + "///");

        byte[] downloaded = serviceWithSlashes.download("stored.jpg");

        assertThat(downloaded).containsExactly(1, 2, 3);
        assertThat(requests.get(0).path()).isEqualTo("/files/stored.jpg");
    }
}
