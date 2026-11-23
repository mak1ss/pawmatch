package com.pawmatch.recommendation.domain;

import java.util.List;

/** Assessment of specific breeds against a profile, in the requested order. Excluded breeds have no index. */
public record ComparisonResult(String modelVersion, List<ScoringResult.ScoredBreed> breeds) {

    public ComparisonResult {
        breeds = List.copyOf(breeds);
    }
}
