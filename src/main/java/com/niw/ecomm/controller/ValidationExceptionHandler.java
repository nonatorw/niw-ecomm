package com.niw.ecomm.controller;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that maps Bean Validation failures to uniform HTTP
 * 400 responses.
 *
 * <p>
 * Handles two exception types that arise from different validation mechanisms
 * in Spring MVC:
 * <ul>
 *     <li>{@link ConstraintViolationException} — thrown by Spring AOP when
 *          path-variable or method-parameter constraints (e.g. 
 *          {@code @Positive}, {@code @NotBlank}) are violated on a
 *          {@link org.springframework.validation.annotation.Validated} class</li>
 *     <li>{@link MethodArgumentNotValidException} — thrown by Spring MVC when a
 *         {@code @Valid @RequestBody} argument fails Bean Validation</li>
 * </ul>
 *
 * <p>
 * Both exception types produce the same response shape so that clients receive
 * a consistent error contract regardless of where in the request pipeline the
 * violation was detected:
 * 
 * <pre>{@code {"error": "fieldName: constraint message, ..."}}</pre>
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

  /**
   * Maps path-variable and method-parameter constraint violations to HTTP 400.
   *
   * <p>
   * Strips the fully qualified method path from each violation's property path
   * (e.g. {@code OrderController.getById.id} → {@code id}) so that internal
   * implementation details are not leaked to clients.
   *
   * @param ex the exception containing one or more constraint violations
   * @return a map with a single {@code "error"} key whose value is a
   *         comma-separated list of {@code "field: message"} pairs
   */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handle(ConstraintViolationException ex) {
    String errors = ex.getConstraintViolations()
                      .stream()
                      .map(this::mapErrors)
                      .collect(Collectors.joining(", "));

    return Map.of("error", errors);
  }

  private String mapErrors(ConstraintViolation<?> v) {
    String path = v.getPropertyPath().toString();
    String field = path.contains(".")
        ? path.substring(path.lastIndexOf('.') + 1)
        : path;

    return field + ": " + v.getMessage();
  }

  /**
   * Maps request-body field validation failures to HTTP 400.
   *
   * <p>
   * Extracts field errors from the binding result so that each entry identifies
   * the offending field and its associated constraint message.
   *
   * @param ex the exception containing binding errors from
   *           {@code @Valid @RequestBody} processing
   * @return a map with a single {@code "error"} key whose value is a
   *         comma-separated list of {@code "field: message"} pairs
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handle(MethodArgumentNotValidException ex) {
    String errors = ex.getBindingResult()
                      .getFieldErrors()
                      .stream()
                      .map(e -> e.getField() + ": " + e.getDefaultMessage())
                      .collect(Collectors.joining(", "));

    return Map.of("error", errors);
  }
}
