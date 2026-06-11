package Bioracer.BachelorProject.Backend.pipeline.adapters;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class KlingAdapter {

    private static final long POLL_INTERVAL_MS = 5_000;
    private static final int DEFAULT_DURATION = 5;

    private static final String DEFAULT_NEGATIVE_PROMPT = "blur, distort, low quality, warping, flicker";
    private static final String DEFAULT_PROMPT = "The model stands and rotates and slowly turns 180 degrees, smoothly "
            + "revealing the front, side and back of the outfit. Studio lighting, clean seamless "
            + "background, steady locked-off camera, professional product showcase, photorealistic.";

    private final RestClient client;
    private final String modelId;
    private final long timeoutSeconds;

    public KlingAdapter(String apiKey, String modelId, String queueBaseUrl, long timeoutSeconds) {
        this.client = RestClient.builder()
                .baseUrl(queueBaseUrl)
                .defaultHeader("Authorization", "Key " + apiKey)
                .build();
        this.modelId = modelId;
        this.timeoutSeconds = timeoutSeconds;
    }

    @SuppressWarnings("unchecked")
    public byte[] generate(byte[] startImage, byte[] endImage, int durationSeconds, String prompt) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("start_image_url", toBase64DataUri(startImage));
        input.put("end_image_url", toBase64DataUri(endImage));
        input.put("prompt", prompt != null ? prompt : DEFAULT_PROMPT);
        input.put("negative_prompt", DEFAULT_NEGATIVE_PROMPT);
        input.put("duration", durationSeconds > 0 ? String.valueOf(durationSeconds) : String.valueOf(DEFAULT_DURATION));
        input.put("generate_audio", false);

        Map<String, Object> submitResponse = (Map<String, Object>) client.post()
                .uri("/" + modelId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(input)
                .retrieve()
                .body(Map.class);

        String requestId = (String) submitResponse.get("request_id");
        String statusUrl = (String) submitResponse.get("status_url");
        String responseUrl = (String) submitResponse.get("response_url");

        System.out.println("Submitted request: " + requestId);

        return pollForResult(requestId, statusUrl, responseUrl);
    }

    private static String toBase64DataUri(byte[] imageBytes) {
        String mimeType = detectMimeType(imageBytes);
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    private static String detectMimeType(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == (byte) 0x50 // P
                && bytes[2] == (byte) 0x4E // N
                && bytes[3] == (byte) 0x47) { // G
            return "image/png";
        }
        if (bytes.length >= 4
                && bytes[0] == (byte) 0x52 // R
                && bytes[1] == (byte) 0x49 // I
                && bytes[2] == (byte) 0x46 // F
                && bytes[3] == (byte) 0x46) { // F
            return "image/webp";
        }
        return "image/jpeg"; // fallback
    }

    @SuppressWarnings("unchecked")
    private byte[] pollForResult(String requestId, String statusUrl, String responseUrl) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1_000L;

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrupted for request: " + requestId, e);
            }

            Map<String, Object> statusResponse = (Map<String, Object>) client.get()
                    .uri(URI.create(statusUrl))
                    .retrieve()
                    .body(Map.class);

            String status = (String) statusResponse.get("status");
            System.out.println("Request " + requestId + " status: " + status);

            if ("COMPLETED".equals(status)) {
                Map<String, Object> result = (Map<String, Object>) client.get()
                        .uri(URI.create(responseUrl))
                        .retrieve()
                        .body(Map.class);

                Map<String, Object> video = (Map<String, Object>) result.get("video");
                String videoUrl = (String) video.get("url");

                System.out.println("Downloading video from: " + videoUrl);
                return RestClient.create().get()
                        .uri(URI.create(videoUrl))
                        .retrieve()
                        .body(byte[].class);
            }

            if (status != null && !"IN_QUEUE".equals(status) && !"IN_PROGRESS".equals(status)) {
                throw new RuntimeException("fal.ai request " + requestId + " ended in state '" + status + "'");
            }
        }

        throw new RuntimeException(
                "fal.ai video generation timed out after " + timeoutSeconds + "s for request: " + requestId);
    }
}