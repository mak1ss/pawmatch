package com.pawmatch.recommendation.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.pawmatch.recommendation.domain.RecommendationRun;

/** Stores recommendation runs with their ranked items. */
public interface RecommendationRunRepository {

    /** Stores a new run (its id must be null) and returns it with id and creation time. */
    RecommendationRun save(RecommendationRun run);

    Optional<RecommendationRun> findLatestBySessionId(UUID sessionId);
}
