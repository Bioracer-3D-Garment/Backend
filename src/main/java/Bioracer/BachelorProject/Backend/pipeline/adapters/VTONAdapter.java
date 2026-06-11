package Bioracer.BachelorProject.Backend.pipeline.adapters;

import Bioracer.BachelorProject.Backend.pipeline.models.AdvancedSettings;

public interface VTONAdapter {

    byte[] generate(byte[] designBytes,
            byte[] personImageBytes,
            AdvancedSettings advancedSettings);
}
