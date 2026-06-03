package Bioracer.BachelorProject.Backend.pipeline.models;

public class AdvancedSettings{
        String resolution;
        String outputFormat;
        String prompt;

        public AdvancedSettings(String resolution, String outputFormat, String prompt){
            this.resolution = resolution;
            this.outputFormat = outputFormat;
            this.prompt = prompt;
        }

        public String getResolution() {
            return resolution;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public String getPrompt() {
            return prompt;
        }
}