package com.innowise.userservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail, WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
    ProblemDetail problem = createProblemDetail(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(CardNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleCardNotFound(CardNotFoundException ex, WebRequest request) {
    ProblemDetail problem = createProblemDetail(HttpStatus.NOT_FOUND, "Card Not Found", ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(MaximumCardsLimitExceededException.class)
  public ResponseEntity<ProblemDetail> handleMaxCardsLimit(MaximumCardsLimitExceededException ex, WebRequest request) {
    ProblemDetail problem = createProblemDetail(HttpStatus.CONFLICT, "Maximum Cards Limit Exceeded", ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
    ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "One or more parameters haven't passed validation",
            request
    );
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.toList());
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(DuplicateUserException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateUser(DuplicateUserException ex, WebRequest request) {
    ProblemDetail problem = createProblemDetail(HttpStatus.CONFLICT, "User Already Exists", ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  public ResponseEntity<ProblemDetail> handleAccessDenied(RuntimeException ex, WebRequest request) {
    ProblemDetail problem = createProblemDetail(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "You don't have permission to access this resource",
            request
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleAllExceptions(Exception ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }
}
