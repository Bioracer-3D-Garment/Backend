package Bioracer.BachelorProject.Backend.pipeline.adapters;

public interface VTONAdapter {

    /**
     * @param frontDesignBytes  raw PNG bytes of the front garment image
     * @param backDesignBytes   raw PNG bytes of the back garment image
     * @param personImageBytes  raw PNG bytes of the model/pose image
     * @param category          one of: upper_body, lower_body, dresses
     * @param prompt            optional instruction string; adapter uses its default when null
     * @return generated result image as raw PNG bytes
     */
    byte[] generate(byte[] frontDesignBytes,
                    byte[] backDesignBytes,
                    byte[] personImageBytes,
                    String category,
                    String prompt);
}
