package Bioracer.BachelorProject.Backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a .env file from the working directory into Spring's Environment as a low-priority
 * property source. Existing system environment variables and JVM system properties take
 * precedence because this source is added last. Has no effect if .env is absent.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DotEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        File dotEnv = new File(".env");
        if (!dotEnv.exists()) return;

        Map<String, Object> props = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(dotEnv.toPath())) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 1) continue;
                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                // strip matching surrounding quotes added by some .env editors
                if (value.length() >= 2) {
                    char first = value.charAt(0);
                    char last  = value.charAt(value.length() - 1);
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length() - 1);
                    }
                }
                props.put(key, value);
            }
        } catch (Exception e) {
            // silently skip — .env is an optional dev convenience
            return;
        }

        // addLast so system env / JVM props always win
        environment.getPropertySources().addLast(new MapPropertySource("dotenv", props));
    }
}
