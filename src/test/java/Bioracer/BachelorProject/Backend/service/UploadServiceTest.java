package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UploadServiceTest {

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        // No HTTP calls are made in these tests; only the guard clauses are exercised.
        uploadService = new UploadService("http://localhost:9999");
    }

    @Test
    void uploadThrowsWhenBytesAreNull() {
        assertThatThrownBy(() -> uploadService.upload(null, "file.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot upload empty file");
    }

    @Test
    void uploadThrowsWhenBytesAreEmpty() {
        assertThatThrownBy(() -> uploadService.upload(new byte[0], "file.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot upload empty file");
    }

    @Test
    void uploadThrowsWhenFilenameIsBlank() {
        assertThatThrownBy(() -> uploadService.upload(new byte[] { 1 }, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename is required for upload");
    }

    @Test
    void uploadThrowsWhenFilenameIsNull() {
        assertThatThrownBy(() -> uploadService.upload(new byte[] { 1 }, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename is required for upload");
    }

    @Test
    void uploadVideoThrowsWhenBytesAreEmpty() {
        assertThatThrownBy(() -> uploadService.uploadVideo(new byte[0], "video"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot upload empty file");
    }

    @Test
    void uploadVideoThrowsWhenFilenameIsBlank() {
        assertThatThrownBy(() -> uploadService.uploadVideo(new byte[] { 1 }, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename is required for upload");
    }

    @Test
    void downloadThrowsWhenReferenceIsNull() {
        assertThatThrownBy(() -> uploadService.download(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot download file: reference is blank");
    }

    @Test
    void downloadThrowsWhenReferenceIsBlank() {
        assertThatThrownBy(() -> uploadService.download(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot download file: reference is blank");
    }

    @Test
    void deleteThrowsWhenReferenceIsNull() {
        assertThatThrownBy(() -> uploadService.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete file: reference is blank");
    }

    @Test
    void deleteThrowsWhenReferenceIsBlank() {
        assertThatThrownBy(() -> uploadService.delete(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete file: reference is blank");
    }

    @Test
    void uploadVideoThrowsWhenBytesAreNull() {
        assertThatThrownBy(() -> uploadService.uploadVideo(null, "video"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot upload empty file");
    }

    @Test
    void uploadVideoThrowsWhenFilenameIsNull() {
        assertThatThrownBy(() -> uploadService.uploadVideo(new byte[] { 1 }, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename is required for upload");
    }

    @Test
    void downloadVideoThrowsWhenReferenceIsBlank() {
        assertThatThrownBy(() -> uploadService.downloadVideo(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot download file: reference is blank");
    }

    @Test
    void deleteVideoThrowsWhenReferenceIsBlank() {
        assertThatThrownBy(() -> uploadService.deleteVideo(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete file: reference is blank");
    }
}
