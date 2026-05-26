package Bioracer.BachelorProject.Backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public record UploadResult(String secureUrl, String publicId, String thumbnailUrl) {}

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
}
