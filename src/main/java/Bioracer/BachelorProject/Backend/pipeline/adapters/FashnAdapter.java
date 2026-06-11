package Bioracer.BachelorProject.Backend.pipeline.adapters;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import Bioracer.BachelorProject.Backend.pipeline.models.AdvancedSettings;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FashnAdapter implements VTONAdapter {

    private static final long POLL_INTERVAL_MS = 3_000;
    private final RestClient client;
    private final long timeoutSeconds;

    public FashnAdapter(String apiKey, String baseUrl, long timeoutSeconds) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] generate(byte[] designBytes,
            byte[] personImageBytes,
            AdvancedSettings advancedSettings) {
        String modelImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(personImageBytes);
        String productImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(designBytes);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("model_image", modelImage);
        inputs.put("product_image", productImage);
        inputs.put("prompt", advancedSettings.getPrompt());
        inputs.put("resolution", advancedSettings.getResolution());
        inputs.put("generation_mode", "balanced");
        inputs.put("num_images", 1);
        inputs.put("output_format", advancedSettings.getOutputFormat());

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model_name", "tryon-max");
        requestBody.put("inputs", inputs);

        // Step 1: submit prediction
        Map<String, Object> submitResponse = (Map<String, Object>) client.post()
                .uri("/run")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (submitResponse == null || submitResponse.get("id") == null) {
            throw new RuntimeException("Fashn.ai /run returned no prediction id"
                    + (submitResponse != null ? ": " + submitResponse.get("error") : ""));
        }
        String predictionId = (String) submitResponse.get("id");

        // Step 2: poll until completed, failed, or timed out
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1_000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrupted for prediction: " + predictionId, e);
            }

            Map<String, Object> statusResponse = (Map<String, Object>) client.get()
                    .uri("/status/{id}", predictionId)
                    .retrieve()
                    .body(Map.class);

            String status = (String) statusResponse.get("status");

            if ("completed".equals(status)) {
                List<String> outputs = (List<String>) statusResponse.get("output");
                String imageUrl = outputs.get(0);
                return RestClient.create().get()
                        .uri(imageUrl)
                        .retrieve()
                        .body(byte[].class);
            }

            if ("failed".equals(status)) {
                throw new RuntimeException("Fashn.ai prediction failed for id " + predictionId
                        + ": " + statusResponse.get("error"));
            }
        }

        throw new RuntimeException(
                "Fashn.ai prediction timed out after " + timeoutSeconds + "s for id: " + predictionId);
    }
}
