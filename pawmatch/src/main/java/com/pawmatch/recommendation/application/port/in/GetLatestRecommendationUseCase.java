package com.pawmatch.recommendation.application.port.in;

import java.util.UUID;

import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.recommendation.domain.RecommendationRun;

/** The most recent recommendation run of a session, e.g. to show the result again after a page reload. */
public interface GetLatestRecommendationUseCase {

    /**
     * @throws ResourceNotFoundException if the session has no recommendation run
     */
    RecommendationRun getLatestRecommendation(UUID sessionId);
}
