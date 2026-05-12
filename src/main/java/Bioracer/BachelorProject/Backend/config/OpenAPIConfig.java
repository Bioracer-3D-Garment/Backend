package Bioracer.BachelorProject.Backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

        @Value("${springdoc.server-url}")
        private String serverUrl;

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Bioracer Backend")
                                                .description("API documentation for managing events and tickets")
                                                .version("1.0.0"))
                                .addServersItem(new Server().url(serverUrl));
        }
}
