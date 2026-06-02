package Bioracer.BachelorProject.Backend.pipeline.adapters;

import org.springframework.web.client.RestClient;

/**
 * Placeholder for Kling AI integration.
 *
 * Note: Kling produces video (image-to-video), not a try-on image. The shared VTONAdapter
 * interface is intentional so this adapter is swappable via vton.adapter=kling in
 * application.properties. When implemented, generate() returns video file bytes.
 * Future work may split VTONAdapter into separate image/video interfaces.
 */
public class KlingAdapter implements VTONAdapter {

    @SuppressWarnings("unused")
    private final RestClient client;

    public KlingAdapter(String apiKey) {
        // TODO: replace base URL once Kling endpoint details are finalized
        this.client = RestClient.builder()
                .baseUrl("https://api.kling.ai")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public byte[] generate(byte[] frontDesignBytes,
                           byte[] backDesignBytes,
                           byte[] personImageBytes,
                           String category,
                           String prompt) {
        // TODO: implement Kling AI video generation when the endpoint details are finalized.
        // Returns video file bytes (not image bytes) — see class-level note.
        throw new UnsupportedOperationException(
                "KlingAdapter is not yet implemented — endpoint details pending.");
    }
}
