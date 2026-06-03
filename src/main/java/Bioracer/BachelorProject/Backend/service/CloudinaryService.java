package Bioracer.BachelorProject.Backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    public record UploadResult(String secureUrl, String publicId, String thumbnailUrl) {
    }

    public UploadResult upload(byte[] imageBytes, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image"));
            String secureUrl = (String) result.get("secure_url");
            String returnedPublicId = (String) result.get("public_id");
            String thumbnailUrl = deriveThumbnailUrl(secureUrl);
            return new UploadResult(secureUrl, returnedPublicId, thumbnailUrl);
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed for publicId=" + publicId, e);
        }
    }

    /**
     * Uploads a generated video (e.g. an MP4 from the Kling pipeline) under the given public ID.
     * Unlike {@link #upload}, this uses {@code resource_type=video} and derives a poster-frame
     * thumbnail (first frame, scaled) for previews.
     */
    public UploadResult uploadVideo(byte[] videoBytes, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(videoBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "video"));
            String secureUrl = (String) result.get("secure_url");
            String returnedPublicId = (String) result.get("public_id");
            String thumbnailUrl = deriveVideoThumbnailUrl(secureUrl);
            return new UploadResult(secureUrl, returnedPublicId, thumbnailUrl);
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary video upload failed for publicId=" + publicId, e);
        }
    }

    /**
     * Resolves a Cloudinary public ID to its secure delivery URL and downloads the image bytes.
     * Used by the generation pipeline to fetch a model's pose images.
     */
    public byte[] download(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("Cannot download Cloudinary image: publicId is blank");
        }
        try {
            String url = cloudinary.url().secure(true).generate(publicId);
            byte[] bytes = RestClient.create().get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("Cloudinary returned empty body for publicId=" + publicId);
            }
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary download failed for publicId=" + publicId, e);
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary delete failed for publicId=" + publicId, e);
        }
    }

    private String deriveThumbnailUrl(String secureUrl) {
        if (!secureUrl.contains("/upload/")) {
            throw new IllegalArgumentException("Cloudinary URL missing /upload/ segment: " + secureUrl);
        }
        return secureUrl.replace("/upload/", "/upload/w_400,h_400,c_fit/");
    }

    /**
     * Derives a poster-frame thumbnail (JPG) from a Cloudinary video delivery URL by scaling the
     * first frame and swapping the video extension for {@code .jpg}.
     */
    private String deriveVideoThumbnailUrl(String secureUrl) {
        if (!secureUrl.contains("/upload/")) {
            throw new IllegalArgumentException("Cloudinary URL missing /upload/ segment: " + secureUrl);
        }
        String scaled = secureUrl.replace("/upload/", "/upload/so_0,w_400,h_400,c_fit/");
        int lastDot = scaled.lastIndexOf('.');
        return (lastDot > scaled.lastIndexOf('/')) ? scaled.substring(0, lastDot) + ".jpg" : scaled + ".jpg";
    }
}
