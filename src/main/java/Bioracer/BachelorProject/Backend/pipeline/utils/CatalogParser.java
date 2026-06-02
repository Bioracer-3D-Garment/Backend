package Bioracer.BachelorProject.Backend.pipeline.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a CSV catalog file (columns: product_id, garment_type, flat_lay_image_path,
 * garment_description) into a list of CatalogProduct objects.
 *
 * Note: the primary ingestion path is now the multipart asset generation endpoint in AssetGenerationController.
 * This parser is retained for CLI/scripted asset-generation use.
 */
@Component
public class CatalogParser {

    private static final Logger log = LoggerFactory.getLogger(CatalogParser.class);

    /**
     * The first row is treated as a header and skipped.
     * Rows missing product_id or flat_lay_image_path are skipped with a warning.
     * garment_type is used as the Fashn.ai category (upper_body / lower_body / dresses).
     */
    public List<CatalogProduct> parse(InputStream is) throws IOException {
        List<CatalogProduct> products = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                log.warn("CSV file is empty");
                return products;
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                // limit=4 so garment_description may contain commas
                String[] parts = line.split(",", 4);
                if (parts.length < 3) {
                    log.warn("Skipping malformed CSV row at line {} (expected at least 3 columns): {}",
                            lineNumber, line);
                    continue;
                }

                String productId        = parts[0].trim();
                String garmentType      = parts[1].trim();
                String flatLayImagePath = parts[2].trim();

                if (productId.isBlank() || flatLayImagePath.isBlank()) {
                    log.warn("Skipping row at line {} — product_id or flat_lay_image_path is blank",
                            lineNumber);
                    continue;
                }

                try {
                    byte[] imageBytes = Files.readAllBytes(Paths.get(flatLayImagePath));
                    products.add(new CatalogProduct(productId, garmentType, imageBytes));
                } catch (IOException e) {
                    log.warn("Skipping row at line {} — cannot read image at {}: {}",
                            lineNumber, flatLayImagePath, e.getMessage());
                }
            }
        }
        log.info("Parsed {} valid products from CSV", products.size());
        return products;
    }
}
