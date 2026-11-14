package com.pawmatch.common.kernel;

/**
 * One invalid input field, returned in the {@code field_errors} list of a 422 response.
 *
 * @param field   snake_case field name as seen by the API client (e.g. {@code activity_level})
 * @param message human-readable message in Ukrainian
 */
public record FieldError(String field, String message) {
}
