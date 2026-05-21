package Bioracer.BachelorProject.Backend.pipeline.adapters;

public interface VTONAdapter {

    /**
     * @param garmentImageBytes raw PNG bytes of the flat-lay garment image
     * @param personImageBytes  raw PNG bytes of the model/pose image
     * @param category          one of: upper_body, lower_body, dresses
     * @param prompt            optional instruction string; adapter uses its default when null
     * @return generated result image as raw PNG bytes
     */
    byte[] generate(byte[] garmentImageBytes, byte[] personImageBytes, String category, String prompt);
}
