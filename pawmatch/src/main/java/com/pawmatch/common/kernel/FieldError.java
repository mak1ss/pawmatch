package com.pawmatch.common.kernel;

import java.util.Map;
import java.util.Objects;

/**
 * One invalid input field, returned in the {@code field_errors} list of a 422 response.
 * <p>
 * Clients should build their own wording from {@code code} and {@code params}; {@code message} is a ready-made
 * Ukrainian fallback.
 *
 * @param field   snake_case field name as seen by the API client (e.g. {@code activity_level}, {@code hard_limits.energy_level})
 * @param code    stable machine-readable error code (e.g. {@code required}, {@code invalid_integer})
 * @param message human-readable fallback message in Ukrainian
 * @param params  values the client needs to explain the error (e.g. {@code min}, {@code max}, {@code allowed});
 *                {@code null} when there are none
 */
public record FieldError(String field, String code, String message, Map<String, Object> params) {

    public FieldError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        params = params == null || params.isEmpty() ? null : Map.copyOf(params);
    }

    public FieldError(String field, String code, String message) {
        this(field, code, message, null);
    }
}
