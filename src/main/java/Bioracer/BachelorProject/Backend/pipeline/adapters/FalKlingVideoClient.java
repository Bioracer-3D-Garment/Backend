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
public class FalKlingVideoClient {

    /** Default prompt: the model turns around to reveal both the front and back of the garment. */
    private static final String DEFAULT_PROMPT =
            "The model stands in a studio and slowly turns around 180 degrees, first facing the " +
            "camera to show the front of the garment, then rotating to reveal the back. " +
            "Smooth, natural motion with consistent lighting. Preserve the garment's text, logos, " +
            "colors, patterns, and the model's appearance exactly.";

    /** Appended to the default prompt when a reference element (e.g. side view) is supplied. */
    private static final String ELEMENT_HINT =
            " The model's side profile is provided as @Element1 — use it as reference for accurate side detail.";

    private static final String DEFAULT_NEGATIVE_PROMPT = "blur, distort, low quality, warping, flicker";

    private static final int MIN_DURATION = 3;
    private static final int MAX_DURATION = 15;
    private static final int DEFAULT_DURATION = 5;

    private final RestClient client;
    private final String submitUrl;
    private final long timeoutSeconds;
    private final long pollIntervalMs;

    public FalKlingVideoClient(String apiKey,
                               String modelId,
                               String queueBaseUrl,
                               long timeoutSeconds,
                               long pollIntervalMs) {
        this.client = RestClient.builder()
                .baseUrl(queueBaseUrl)
                .defaultHeader("Authorization", "Key " + apiKey)
                .build();
        // Build the absolute submit URL up front. The model id contains slashes
        // (e.g. "fal-ai/kling-video/v3/pro/image-to-video"); passing it as a RestClient
        // path variable would percent-encode those slashes to %2F and produce a 404.
        this.submitUrl = queueBaseUrl.replaceAll("/+$", "") + "/" + modelId;
        this.timeoutSeconds = timeoutSeconds;
        this.pollIntervalMs = pollIntervalMs;
    }

    /**
     * Generates a video and returns the raw MP4 bytes.
     *
     * @param startImageUrl       URL of the start frame (front try-on image) — required
     * @param endImageUrl         URL of the end frame (back try-on image) — optional, may be null
     * @param referenceImageUrls  extra reference images (e.g. side view) — may be null/empty
     * @param durationSeconds     desired duration (3–15); null falls back to the default
     * @param prompt              optional instruction; null falls back to the default turntable prompt
     */
    @SuppressWarnings("unchecked")
    public byte[] generate(String startImageUrl,
                           String endImageUrl,
                           List<String> referenceImageUrls,
                           Integer durationSeconds,
                           String prompt) {

        if (startImageUrl == null || startImageUrl.isBlank()) {
            throw new IllegalArgumentException("startImageUrl (front frame) is required");
        }

        boolean hasReferences = referenceImageUrls != null && !referenceImageUrls.isEmpty();
        boolean usingDefaultPrompt = (prompt == null || prompt.isBlank());

        // When we supply the default prompt and have a reference element, point the model at it
        // via @Element1. If the caller supplies their own prompt, we leave their @Element refs alone.
        String effectivePrompt = usingDefaultPrompt ? DEFAULT_PROMPT : prompt;
        if (usingDefaultPrompt && hasReferences) {
            effectivePrompt += ELEMENT_HINT;
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("start_image_url", startImageUrl);
        if (endImageUrl != null && !endImageUrl.isBlank()) {
            input.put("end_image_url", endImageUrl);
        }
        input.put("prompt", effectivePrompt);
        input.put("negative_prompt", DEFAULT_NEGATIVE_PROMPT);
        input.put("duration", String.valueOf(clampDuration(durationSeconds)));
        // No spoken audio for product turntables — the model defaults this to true otherwise.
        input.put("generate_audio", false);

        // Extra views (e.g. side) ride along as an image-set element, referenced in the prompt
        // as @Element1. Same model + same garment as the start/end frames, so identity stays
        // consistent without feeding garment-less reference photos.
        if (referenceImageUrls != null && !referenceImageUrls.isEmpty()) {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("frontal_image_url", referenceImageUrls.get(0));
            element.put("reference_image_urls", referenceImageUrls);
            input.put("elements", List.of(element));
        }

        // Step 1: submit to the queue. Use an absolute URI so the slashes in the model id
        // are kept as path separators (a path variable would encode them to %2F → 404).
        Map<String, Object> submit = (Map<String, Object>) client.post()
                .uri(URI.create(submitUrl))
                .contentType(MediaType.APPLICATION_JSON)
                .body(input)
                .retrieve()
                .body(Map.class);

        if (submit == null) {
            throw new RuntimeException("fal.ai returned an empty submit response");
        }
        String requestId = (String) submit.get("request_id");
        String statusUrl = (String) submit.get("status_url");
        String responseUrl = (String) submit.get("response_url");
        if (statusUrl == null || responseUrl == null) {
            throw new RuntimeException("fal.ai submit response missing status_url/response_url for request: " + requestId);
        }

        // Step 2: poll until COMPLETED, failed, or timed out.
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1_000L;
        while (System.currentTimeMillis() < deadline) {
            sleep(requestId);

            Map<String, Object> status = (Map<String, Object>) client.get()
                    .uri(URI.create(statusUrl))
                    .retrieve()
                    .body(Map.class);

            String state = status != null ? (String) status.get("status") : null;
            if ("COMPLETED".equals(state)) {
                return fetchVideoBytes(responseUrl, requestId);
            }
            if (state != null && !"IN_QUEUE".equals(state) && !"IN_PROGRESS".equals(state)) {
                throw new RuntimeException("fal.ai request " + requestId + " ended in state '" + state + "'");
            }
        }

        throw new RuntimeException(
                "fal.ai video generation timed out after " + timeoutSeconds + "s for request: " + requestId);
    }

    @SuppressWarnings("unchecked")
    private byte[] fetchVideoBytes(String responseUrl, String requestId) {
        Map<String, Object> result = (Map<String, Object>) client.get()
                .uri(URI.create(responseUrl))
                .retrieve()
                .body(Map.class);

        Object videoObj = result != null ? result.get("video") : null;
        if (!(videoObj instanceof Map)) {
            throw new RuntimeException("fal.ai result missing 'video' object for request: " + requestId);
        }
        String videoUrl = (String) ((Map<String, Object>) videoObj).get("url");
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new RuntimeException("fal.ai result missing video url for request: " + requestId);
        }

        byte[] bytes = RestClient.create().get()
                .uri(URI.create(videoUrl))
                .retrieve()
                .body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("Downloaded video was empty for request: " + requestId);
        }
        return bytes;
    }

    private void sleep(String requestId) {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Polling interrupted for fal.ai request: " + requestId, e);
        }
    }

    private static int clampDuration(Integer durationSeconds) {
        if (durationSeconds == null) {
            return DEFAULT_DURATION;
        }
        return Math.max(MIN_DURATION, Math.min(MAX_DURATION, durationSeconds));
    }
}
