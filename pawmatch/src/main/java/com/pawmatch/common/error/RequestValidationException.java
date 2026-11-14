package com.pawmatch.common.error;

import java.util.List;

/**
 * Thrown by domain validation (e.g. questionnaire checks) after collecting <b>all</b> field errors.
 * Mapped to HTTP 422 with a {@code field_errors} list.
 */
public class RequestValidationException extends RuntimeException {

    private final List<FieldError> fieldErrors;

    public RequestValidationException(List<FieldError> fieldErrors) {
        super("Request validation failed: " + fieldErrors.size() + " field error(s)");
        if (fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("fieldErrors must not be empty");
        }
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
