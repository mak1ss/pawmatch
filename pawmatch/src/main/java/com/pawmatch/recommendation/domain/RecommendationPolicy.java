package com.pawmatch.recommendation.domain;

import java.math.BigDecimal;

/** MVP product rules for recommendations (board #9643228, #9643300): at most 5 breeds, index threshold 60. */
public final class RecommendationPolicy {

    public static final int LIMIT = 5;
    public static final BigDecimal MIN_SCORE = BigDecimal.valueOf(60);

    private RecommendationPolicy() {
    }
}
