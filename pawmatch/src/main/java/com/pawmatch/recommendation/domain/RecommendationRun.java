package com.pawmatch.recommendation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One stored recommendation: the engine's result for a specific answers version and catalogue version.
 *
 * @param id     null until stored
 * @param items  the recommended breeds by rank, with their catalogue ids (for metrics and feedback)
 */
public record RecommendationRun(
        UUID id,
        UUID sessionId,
        int answersVersion,
        String datasetVersion,
        String engineUsed,
        boolean fallbackUsed,
        long latencyMs,
        Instant createdAt,
        ScoringResult result,
        List<RankedBreed> items) {

    public RecommendationRun {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(datasetVersion, "datasetVersion");
        Objects.requireNonNull(engineUsed, "engineUsed");
        Objects.requireNonNull(result, "result");
        items = List.copyOf(items);
    }

    /**
     * @param rank      1-based position in the recommendations
     * @param breedUuid the breed's internal id in the catalogue
     * @param breedId   the breed slug
     */
    public record RankedBreed(int rank, UUID breedUuid, String breedId, BigDecimal compatibilityIndex) {
    }
}
