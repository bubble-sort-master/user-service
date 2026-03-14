package com.innowise.userservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private ProblemDetail createProblemDetail(
          HttpStatus status,
          String title,
          String detail,
          WebRequest request) {

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleUserNotFound(
          UserNotFoundException ex,
          WebRequest request) {

    log.warn("User not found: id = {}, message: {}",
            extractId(ex.getMessage()), ex.getMessage(), ex);

    ProblemDetail problem = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "User Not Found",
            ex.getMessage(),
            request
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(CardNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleCardNotFound(
          CardNotFoundException ex,
          WebRequest request) {

    log.warn("Card Not Found: id = {}, message: {}",
            extractId(ex.getMessage()), ex.getMessage(), ex);

    ProblemDetail problem = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "Card Not Found",
            ex.getMessage(),
            request
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(MaximumCardsLimitExceededException.class)
  public ResponseEntity<ProblemDetail> handleMaxCardsLimit(
          MaximumCardsLimitExceededException ex,
          WebRequest request) {

    log.warn("Maximum cards limit exceeded: {}", ex.getMessage(), ex);

    ProblemDetail problem = createProblemDetail(
            HttpStatus.CONFLICT,
            "Maximum Cards Limit Exceeded",
            ex.getMessage(),
            request
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
          MethodArgumentNotValidException ex,
          org.springframework.http.HttpHeaders headers,
          HttpStatusCode status,
          WebRequest request) {

    log.info("Validation exception: {}", request.getDescription(false));

    ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "One ore more parameters haven't passed validation",
            request
    );

    List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.toList());

    problem.setProperty("errors", errors);

    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(DuplicateUserException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateUser(
          DuplicateUserException ex,
          WebRequest request) {

    log.warn("Duplicate user attempt: {}", ex.getMessage());

    ProblemDetail problem = createProblemDetail(
            HttpStatus.CONFLICT,
            "User Already Exists",
            ex.getMessage(),
            request
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleAllExceptions(
          Exception ex,
          WebRequest request) {

    log.error("Unhandled exception: {}", ex.getMessage(), ex);

    ProblemDetail problem = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "Unexpected error occurred",
            request
    );

    if (log.isDebugEnabled()) {
      problem.setProperty("trace", ex.getMessage());
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  private Long extractId(String message) {
    try {
      return Long.parseLong(message.replaceAll(".*id: (\\d+).*", "$1"));
    } catch (Exception e) {
      return null;
    }
  }
}