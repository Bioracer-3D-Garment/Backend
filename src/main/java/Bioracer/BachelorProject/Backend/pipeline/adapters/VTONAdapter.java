package Bioracer.BachelorProject.Backend.pipeline.adapters;

import Bioracer.BachelorProject.Backend.pipeline.models.AdvancedSettings;

public interface VTONAdapter {

    /**
     * @param designBytes      raw PNG bytes of the garment/product image for the
     *                         pose being generated (front design for front/side
     *                         poses, back design for the back pose)
     * @param personImageBytes raw PNG bytes of the model/pose image
     * @param advancedSettings optional advanced settings; adapter uses its default
     *                         when null
     * @return generated result image as raw PNG bytes
     */
    byte[] generate(byte[] designBytes,
            byte[] personImageBytes,
            AdvancedSettings advancedSettings);
}
