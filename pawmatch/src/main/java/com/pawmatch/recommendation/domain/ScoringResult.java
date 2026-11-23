package com.pawmatch.recommendation.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Output of a recommendation by a scoring engine. Field names follow the model's JSON contract
 * ({@code pawmatch-baseline-1.0.0}), so the stored result and the API response keep the model's vocabulary.
 *
 * @param scoreMeaning     the index is not a probability; shown to users
 * @param minScore         threshold a breed's index had to reach
 * @param catalogueBreeds  breeds considered
 * @param eligibleBreeds   breeds not excluded by a hard constraint
 * @param aboveThreshold   eligible breeds with an index of at least {@code minScore}
 * @param recommendations  at most {@code limit} breeds, highest index first (ties by breed id)
 * @param notice           shown when fewer than three breeds qualify, otherwise a general hint
 * @param unassessedCriteria answers the engine collected but couldn't score
 * @param excluded         breeds excluded by a hard constraint, sorted by breed id
 */
public record ScoringResult(
        String modelVersion,
        String modelType,
        String scoreMeaning,
        BigDecimal minScore,
        int catalogueBreeds,
        int eligibleBreeds,
        int aboveThreshold,
        List<ScoredBreed> recommendations,
        String notice,
        List<String> unassessedCriteria,
        List<Exclusion> excluded) {

    public ScoringResult {
        recommendations = List.copyOf(recommendations);
        unassessedCriteria = List.copyOf(unassessedCriteria);
        excluded = List.copyOf(excluded);
    }

    /** One assessed breed. {@code compatibilityIndex} is null when the breed is excluded. */
    public record ScoredBreed(
            String breedId,
            String breedName,
            String breedNameEn,
            boolean eligible,
            List<String> exclusionReasons,
            BigDecimal compatibilityIndex,
            List<Factor> factors,
            List<String> reasons,
            List<String> warnings,
            String sourceUrl) {

        public ScoredBreed {
            exclusionReasons = List.copyOf(exclusionReasons);
            factors = List.copyOf(factors);
            reasons = List.copyOf(reasons);
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * One scored criterion.
     *
     * @param breedValue   the breed's rating (Integer) or size code (String)
     * @param profileValue the user's answer: Integer, size code (String) or {@code true} for children/other dogs
     * @param match        0–1, rounded to 4 decimals
     */
    public record Factor(String criterion, String label, Object breedValue, Object profileValue, BigDecimal match,
                         double weight, String explanation) {
    }

    /** A breed excluded before ranking, with every reason. */
    public record Exclusion(String breedId, List<String> reasons) {

        public Exclusion {
            reasons = List.copyOf(reasons);
        }
    }
}
