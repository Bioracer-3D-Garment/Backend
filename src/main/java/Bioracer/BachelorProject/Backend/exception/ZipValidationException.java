package Bioracer.BachelorProject.Backend.exception;

import Bioracer.BachelorProject.Backend.controller.DTO.ZipValidationErrorResponse;

public class ZipValidationException extends RuntimeException {

    private final ZipValidationErrorResponse response;

    public ZipValidationException(ZipValidationErrorResponse response) {
        super(response.message());
        this.response = response;
    }

    public ZipValidationErrorResponse getResponse() {
        return response;
    }
}
