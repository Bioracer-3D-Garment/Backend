package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlers {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(NotFoundException notFoundError) {
        return Map.of(
                "status", "Not found",
                "message", notFoundError.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleAccessDeniedException(AccessDeniedException accessDeniedException) {
        return Map.of(
                "status", "Access Denied",
                "message", accessDeniedException.getMessage());
    }

    @ExceptionHandler(UserException.class)
    @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
    public Map<String, String> handleUserException(UserException userException) {
        return Map.of(
                "status", "Precondition Failed",
                "message", userException.getMessage());
    }
}
