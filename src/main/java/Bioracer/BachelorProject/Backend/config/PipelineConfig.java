package Bioracer.BachelorProject.Backend.config;

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

        return new FashnAdapter(fashnApiKey, fashnBaseUrl, fashnTimeoutSeconds);
    }

    @Bean
    public KlingAdapter klingAdapter(
            @Value("${fal.api.key:}") String falApiKey,
            @Value("${fal.model-id:fal-ai/kling-video/v3/pro/image-to-video}") String falModelId,
            @Value("${fal.queue-base-url:https://queue.fal.run}") String falQueueBaseUrl,
            @Value("${fal.timeout-seconds:600}") long falTimeoutSeconds) {

        return new KlingAdapter(falApiKey, falModelId, falQueueBaseUrl, falTimeoutSeconds);
    }
}
