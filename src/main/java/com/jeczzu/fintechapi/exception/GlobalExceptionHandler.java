package com.jeczzu.fintechapi.exception;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jeczzu.fintechapi.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {

    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(), // "Not Found"
        ex.getMessage(),
        OffsetDateTime.now());

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(error);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {

    ErrorResponse error = new ErrorResponse(
        HttpStatus.CONFLICT.value(),
        HttpStatus.CONFLICT.getReasonPhrase(), // "Conflict"
        ex.getMessage(),
        OffsetDateTime.now());

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .reduce((msg1, msg2) -> msg1 + ", " + msg2)
        .orElse("Validation error");

    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(), // "Bad Request"
        message,
        OffsetDateTime.now());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

    ErrorResponse error = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), // "Internal Server Error"
        "An unexpected error occurred",
        OffsetDateTime.now());

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error);
  }
}