package Bioracer.BachelorProject.Backend.config;

import Bioracer.BachelorProject.Backend.pipeline.adapters.FashnAdapter;
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
}
