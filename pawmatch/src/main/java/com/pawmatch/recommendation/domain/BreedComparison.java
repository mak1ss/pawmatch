package com.pawmatch.recommendation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A comparison of specific breeds against a session's latest answers. Computed on request and not stored.
 *
 * @param answersVersion the answers version that was scored
 * @param datasetVersion the catalogue version the breeds came from
 * @param result         the engine's assessment, breeds in the requested order
 */
public record BreedComparison(
        UUID sessionId,
        int answersVersion,
        String datasetVersion,
        String engineUsed,
        boolean fallbackUsed,
        long latencyMs,
        ComparisonResult result) {

    public BreedComparison {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(datasetVersion, "datasetVersion");
        Objects.requireNonNull(engineUsed, "engineUsed");
        Objects.requireNonNull(result, "result");
    }
}
