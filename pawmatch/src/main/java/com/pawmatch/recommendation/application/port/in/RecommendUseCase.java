package com.pawmatch.recommendation.application.port.in;

import java.util.UUID;

import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.recommendation.domain.RecommendationRun;

/** Scores the latest answers of a session and stores the result as a new recommendation run. */
public interface RecommendUseCase {

    /**
     * @throws ResourceNotFoundException if the session does not exist
     */
    RecommendationRun recommend(UUID sessionId);
}
