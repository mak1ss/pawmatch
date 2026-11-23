package com.pawmatch.recommendation.application.port.in;

import java.util.Map;
import java.util.UUID;

import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.recommendation.domain.BreedComparison;

/** Scores 2–5 chosen breeds against the latest answers of a session, for a side-by-side comparison. Not stored. */
public interface CompareBreedsUseCase {

    /**
     * @param rawRequest JSON-shaped request body: {@code {"breed_ids": [...]}}
     * @throws RequestValidationException listing every problem with the request
     * @throws ResourceNotFoundException  if the session doesn't exist
     */
    BreedComparison compare(UUID sessionId, Map<String, Object> rawRequest);
}
