package Bioracer.BachelorProject.Backend.pipeline.adapters;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for fal.ai's Kling v3 Pro image-to-video model
 * ({@code fal-ai/kling-video/v3/pro/image-to-video}).
 *
 * <p>Produces a "turntable" video of a model: the Fashn-generated <b>front</b> image is the
 * start frame, the <b>back</b> image is the end frame, and any extra views (e.g. <b>side</b>)
 * are passed as reference images via the model's {@code elements} parameter for added detail.
 *
 * <p>Communicates over fal.ai's queue REST API: submit → poll status → fetch result → download
 * the resulting MP4. Authentication uses the {@code Authorization: Key <FAL_KEY>} header.
 * Image inputs are plain URLs (the Cloudinary delivery URLs of the generated try-on images);
 * fal.ai fetches them directly, so no upload/base64 step is required.
 */
public class KlingAdapter {

    private static final long POLL_INTERVAL_MS = 5_000;

    private static final String DEFAULT_NEGATIVE_PROMPT = "blur, distort, low quality, warping, flicker";

    // fal.ai requires a non-empty prompt (unless multi_prompt is used). When the caller doesn't
    // supply one we fall back to this turntable description instead of sending a null prompt,
    // which fal rejects as a missing required field.
    private static final String DEFAULT_TURNTABLE_PROMPT =
            "The model stands on a rotating turntable and slowly turns a full 360 degrees, smoothly "
            + "revealing the front, side and back of the outfit. Studio lighting, clean seamless "
            + "background, steady locked-off camera, professional product showcase, photorealistic.";

    private static final int MIN_DURATION = 3;
    private static final int MAX_DURATION = 15;
    private static final int DEFAULT_DURATION = 5;

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

    /**
     * Generates a video and returns the raw MP4 bytes.
     *
     * @param startImage       StartImage of the start frame (front try-on image) — required
     * @param endImage         URL of the end frame (back try-on image) — optional, may be null
     * @param referenceImageUrls  extra reference images (e.g. side view) — may be null/empty
     * @param durationSeconds     desired duration (3–15); null falls back to the default
     * @param prompt              instruction passed through to the model
     */
    @SuppressWarnings("unchecked")
    public byte[] generate(String startImage,
                           String endImage,
                           List<String> referenceImageUrls,
                           Integer durationSeconds,
                           String prompt) {

        if (startImage == null || startImage.isBlank()) {
            throw new IllegalArgumentException("startImageUrl (front frame) is required");
        }

        boolean hasReferences = referenceImageUrls != null && !referenceImageUrls.isEmpty();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("start_image_url", startImage);
        if (endImage != null && !endImage.isBlank()) {
            input.put("end_image_url", endImage);
        }
        input.put("prompt", (prompt != null && !prompt.isBlank()) ? prompt : DEFAULT_TURNTABLE_PROMPT);
        input.put("negative_prompt", DEFAULT_NEGATIVE_PROMPT);
        input.put("duration", String.valueOf(clampDuration(durationSeconds)));
        // No spoken audio for product turntables — the model defaults this to true otherwise.
        input.put("generate_audio", false);

        // Extra views (e.g. side) ride along as an image-set element, referenced in the prompt
        // as @Element1. Same model + same garment as the start/end frames, so identity stays
        // consistent without feeding garment-less reference photos.
        if (hasReferences) {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("frontal_image_url", referenceImageUrls.get(0));
            element.put("reference_image_urls", referenceImageUrls);
            input.put("elements", List.of(element));
        }

        // Step 1: submit prediction. Build the path literally so the slashes in the model id
        // (e.g. "fal-ai/kling-video/v3/pro/image-to-video") stay as path separators — passing it
        // as a URI template variable percent-encodes them to %2F and 404s on fal's queue.
        Map<String, Object> submitResponse = (Map<String, Object>) client.post()
                .uri(uriBuilder -> uriBuilder.path("/" + modelId).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(input)
                .retrieve()
                .body(Map.class);

        String requestId = (String) submitResponse.get("request_id");
        String statusUrl = (String) submitResponse.get("status_url");
        String responseUrl = (String) submitResponse.get("response_url");

        // Step 2: poll until completed, failed, or timed out
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

            if ("COMPLETED".equals(status)) {
                Map<String, Object> result = (Map<String, Object>) client.get()
                        .uri(URI.create(responseUrl))
                        .retrieve()
                        .body(Map.class);

                Map<String, Object> video = (Map<String, Object>) result.get("video");
                String videoUrl = (String) video.get("url");
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

    private static int clampDuration(Integer durationSeconds) {
        if (durationSeconds == null) {
            return DEFAULT_DURATION;
        }
        return Math.max(MIN_DURATION, Math.min(MAX_DURATION, durationSeconds));
    }
}
