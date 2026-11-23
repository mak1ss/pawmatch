package com.pawmatch.recommendation.application.port.out;

import java.math.BigDecimal;
import java.util.List;

import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.EngineOutcome;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;

/**
 * Scores breeds for a profile. Implementations live in the {@code inference} module; callers never know which engine
 * answered beyond the reported engine id. Engines are pure: everything they need is passed in.
 */
public interface ScoringEngine {

    /** Ranks {@code breeds} for {@code profile}: at most {@code limit} breeds with an index of at least {@code minScore}. */
    EngineOutcome<ScoringResult> recommend(ScoringProfile profile, List<BreedFeatures> breeds, int limit,
                                           BigDecimal minScore);

    /** Assesses exactly the given breeds, in order; excluded breeds are returned without an index. */
    EngineOutcome<ComparisonResult> compare(ScoringProfile profile, List<BreedFeatures> breeds);
}
