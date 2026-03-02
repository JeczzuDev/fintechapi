package com.jeczzu.fintechapi.exception;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  // --- Custom business exceptions ---

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {

    ProblemDetail problem = createProblemDetail(
        HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ProblemDetail> handleConflict(
      ConflictException ex, HttpServletRequest request) {

    ProblemDetail problem = createProblemDetail(
        HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ProblemDetail> handleBusiness(
      BusinessException ex, HttpServletRequest request) {

    HttpStatus status = ex.getStatus();
    ProblemDetail problem = createProblemDetail(
        status, status.getReasonPhrase(), ex.getMessage(), request);
    return ResponseEntity.status(status).body(problem);
  }

  // --- Concurrency ---

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLock(
      ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {

    ProblemDetail problem = createProblemDetail(
        HttpStatus.CONFLICT, "Concurrent Modification",
        "The resource was modified by another request. Please retry.", request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  // --- Validation ---

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .toList();

    ProblemDetail problem = createProblemDetail(
        HttpStatus.BAD_REQUEST, "Validation Failed",
        "One or more fields are invalid", request);
    problem.setProperty("errors", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  // --- Malformed requests ---

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    ProblemDetail problem = createProblemDetail(
        HttpStatus.BAD_REQUEST, "Malformed Request",
        "The request body is missing or contains invalid JSON", request);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    Class<?> requiredType = ex.getRequiredType();
    String expectedType = requiredType != null ? requiredType.getSimpleName() : "unknown";

    String message = "Invalid value '%s' for parameter '%s'. Expected type: %s"
        .formatted(ex.getValue(), ex.getName(), expectedType);

    ProblemDetail problem = createProblemDetail(
        HttpStatus.BAD_REQUEST, "Invalid Parameter", message, request);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  // --- Catch-all ---

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGeneric(
      Exception ex, HttpServletRequest request) {

    log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);

    ProblemDetail problem = createProblemDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
        "An unexpected error occurred", request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  // --- Helper ---

  private ProblemDetail createProblemDetail(
      HttpStatus status, String title, String detail, HttpServletRequest request) {

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", OffsetDateTime.now());
    return problem;
  }
}