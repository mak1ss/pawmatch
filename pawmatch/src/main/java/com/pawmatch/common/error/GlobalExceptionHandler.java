package com.pawmatch.common.error;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps exceptions to RFC 9457 {@link ProblemDetail} responses.
 * <ul>
 *   <li>Validation errors → 422 with {@code field_errors: [{field, message}]} (field names in snake_case).</li>
 *   <li>Unreadable JSON → 400.</li>
 *   <li>Missing resource → 404.</li>
 *   <li>Anything else → 500 without internal details.</li>
 * </ul>
 * Standard Spring MVC exceptions (405, 415, ...) are handled by {@link ResponseEntityExceptionHandler}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String FIELD_ERRORS = "field_errors";

    @ExceptionHandler(RequestValidationException.class)
    ProblemDetail handleRequestValidation(RequestValidationException ex) {
        return validationProblem(ex.getFieldErrors());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Ресурс не знайдено.");
        problem.setTitle("Not Found");
        problem.setProperty("resource", ex.getResource());
        problem.setProperty("id", ex.getId());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Внутрішня помилка сервера. Спробуйте пізніше.");
        problem.setTitle("Internal Server Error");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(toSnakeCase(e.getField()),
                        Objects.requireNonNullElse(e.getDefaultMessage(), "Некоректне значення.")))
                .toList();
        return ResponseEntity.unprocessableContent().body(validationProblem(errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Некоректний JSON у тілі запиту.");
        problem.setTitle("Bad Request");
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail validationProblem(List<FieldError> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, "Запит містить некоректні дані.");
        problem.setTitle("Validation Failed");
        problem.setProperty(FIELD_ERRORS, errors);
        return problem;
    }

    /** {@code activityLevel} → {@code activity_level}; nested paths keep their dots/brackets. */
    static String toSnakeCase(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
