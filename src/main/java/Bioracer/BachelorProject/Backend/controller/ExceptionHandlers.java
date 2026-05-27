package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.ErrorResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.ZipValidationErrorResponse;
import Bioracer.BachelorProject.Backend.exception.ModelException;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.exception.ZipValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        return new ErrorResponse(404, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex) {
        return new ErrorResponse(401, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(UserException.class)
    @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
    public ErrorResponse handleUserException(UserException ex) {
        return new ErrorResponse(412, "Precondition Failed", ex.getMessage());
    }

    @ExceptionHandler(ModelException.class)
    @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
    public ErrorResponse handleModelException(ModelException ex) {
        return new ErrorResponse(412, "Precondition Failed", ex.getMessage());
    }

    @ExceptionHandler(ZipValidationException.class)
    public ResponseEntity<ZipValidationErrorResponse> handleZipValidationException(ZipValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getResponse());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponse handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return new ErrorResponse(413, "Payload Too Large",
                "Upload exceeds the maximum allowed size. Check your ZIP file size.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        HttpStatus httpStatus = HttpStatus.resolve(code);
        String error = httpStatus != null ? httpStatus.getReasonPhrase() : "Error";
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(code).body(new ErrorResponse(code, error, message));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpectedException(Exception ex) {
        return new ErrorResponse(500, "Internal Server Error", ex.getMessage());
    }
}
