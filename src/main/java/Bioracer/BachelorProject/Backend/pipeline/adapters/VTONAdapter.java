package Bioracer.BachelorProject.Backend.pipeline.adapters;

import Bioracer.BachelorProject.Backend.pipeline.models.AdvancedSettings;

public interface VTONAdapter {

    /**
     * @param frontDesignBytes raw PNG bytes of the (front) garment/product image
     * @param personImageBytes raw PNG bytes of the model/pose image
     * @param category         one of: upper_body, lower_body, dresses
     * @param advancedSettings optional advanced settings; adapter uses its default
     *                         when null
     * @return generated result image as raw PNG bytes
     */
    byte[] generate(byte[] frontDesignBytes,
            byte[] personImageBytes,
            byte[] backDesignBytes,
            String category,
            AdvancedSettings advancedSettings);
}
