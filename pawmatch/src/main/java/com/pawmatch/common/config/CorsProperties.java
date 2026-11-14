package com.pawmatch.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Frontend origins allowed to call {@code /api/**}. Set via {@code pawmatch.cors.allowed-origins}
 * (env {@code PAWMATCH_CORS_ALLOWED_ORIGINS}, comma-separated).
 */
@ConfigurationProperties(prefix = "pawmatch.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
