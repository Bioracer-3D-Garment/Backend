package Bioracer.BachelorProject.Backend.pipeline.models;

public class AdvancedSettings{
        String resolution;
        String outputFormat;
        String prompt;

        public AdvancedSettings(){
        }

        public AdvancedSettings(String resolution, String outputFormat, String prompt){
            this.resolution = resolution;
            this.outputFormat = outputFormat;
            this.prompt = prompt;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public void setOutputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
}