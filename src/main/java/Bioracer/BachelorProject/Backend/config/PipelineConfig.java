package Bioracer.BachelorProject.Backend.config;

import Bioracer.BachelorProject.Backend.pipeline.adapters.FalKlingVideoClient;
import Bioracer.BachelorProject.Backend.pipeline.adapters.FashnAdapter;
import Bioracer.BachelorProject.Backend.pipeline.adapters.KlingAdapter;
import Bioracer.BachelorProject.Backend.pipeline.adapters.VTONAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class PipelineConfig {

    @Bean
    public VTONAdapter vtonAdapter(
            @Value("${vton.adapter}") String adapterName,
            @Value("${fashn.api.key:}") String fashnApiKey,
            @Value("${fashn.base-url:https://api.fashn.ai/v1}") String fashnBaseUrl,
            @Value("${fashn.timeout-seconds:120}") long fashnTimeoutSeconds,
            @Value("${kling.api.key:}") String klingApiKey) {

        return switch (adapterName) {
            case "fashn" -> new FashnAdapter(fashnApiKey, fashnBaseUrl, fashnTimeoutSeconds);
            case "kling" -> new KlingAdapter(klingApiKey);
            default -> throw new IllegalArgumentException("Unknown vton.adapter value: '" + adapterName +
                    "'. Supported values: fashn, kling");
        };
    }

    /**
     * Client for fal.ai's Kling v3 Pro image-to-video model. Independent of {@code vton.adapter}:
     * the video pipeline runs as a separate step on top of the Fashn-generated try-on images.
     */
    @Bean
    public FalKlingVideoClient falKlingVideoClient(
            @Value("${fal.api.key:}") String falApiKey,
            @Value("${fal.model-id:fal-ai/kling-video/v3/pro/image-to-video}") String falModelId,
            @Value("${fal.queue-base-url:https://queue.fal.run}") String falQueueBaseUrl,
            @Value("${fal.timeout-seconds:600}") long falTimeoutSeconds,
            @Value("${fal.poll-interval-ms:5000}") long falPollIntervalMs) {

        return new FalKlingVideoClient(
                falApiKey, falModelId, falQueueBaseUrl, falTimeoutSeconds, falPollIntervalMs);
    }
}
