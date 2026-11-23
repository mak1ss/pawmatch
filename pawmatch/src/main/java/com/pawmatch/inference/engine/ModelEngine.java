package com.pawmatch.inference.engine;

import java.math.BigDecimal;
import java.util.List;

import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;

/**
 * One concrete scoring engine (the baseline today, an ML model later). Engines are selected and chained by
 * {@code EngineRouter}, which is what the rest of the application sees as the {@code ScoringEngine} port.
 */
public interface ModelEngine {

    /** Stable id stored with every run, e.g. {@code baseline}. */
    String id();

    ScoringResult recommend(ScoringProfile profile, List<BreedFeatures> breeds, int limit, BigDecimal minScore);

    ComparisonResult compare(ScoringProfile profile, List<BreedFeatures> breeds);
}
