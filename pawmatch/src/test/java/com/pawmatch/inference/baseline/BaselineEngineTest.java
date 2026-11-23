package com.pawmatch.inference.baseline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.pawmatch.questionnaire.domain.AnswersParser;
import com.pawmatch.questionnaire.domain.TestAnswers;
import com.pawmatch.recommendation.application.service.ScoringInputs;
import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.HardLimit;
import com.pawmatch.recommendation.domain.RecommendationPolicy;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;
import com.pawmatch.recommendation.domain.ScoringResult.ScoredBreed;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the baseline, ported from the ML team's {@code test_pawmatch.py}. The validation tests from that suite
 * live with the questionnaire validator ({@code AnswersParserTest}); the corrupt-catalogue test is covered by the
 * catalogue's domain checks and database constraints.
 */
class BaselineEngineTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final BaselineEngine engine = new BaselineEngine();
    private final List<BreedFeatures> catalogue = SeedCatalogue.breeds();

    @Test
    void exampleProfileReturnsRankedExplainedResults() {
        ScoringResult result = recommend(profile(Map.of()), RecommendationPolicy.MIN_SCORE);

        assertThat(result.recommendations()).hasSizeBetween(3, 5);
        assertThat(indexes(result)).isSortedAccordingTo(Collections.reverseOrder());
        assertThat(result.recommendations()).allSatisfy(row -> {
            assertThat(row.compatibilityIndex()).isBetween(BigDecimal.valueOf(60), BigDecimal.valueOf(100));
            assertThat(row.factors()).isNotEmpty();
            assertThat(row.reasons()).isNotEmpty();
            assertThat(row.warnings()).isNotEmpty();
            assertThat(row.sourceUrl()).startsWith("https://www.akc.org/");
        });
        assertThat(result.unassessedCriteria()).isNotEmpty();
    }

    @Test
    void childrenAndOtherDogsAreFilteredBeforeRanking() {
        ScoringResult result = recommend(profile(Map.of("has_other_dogs", true)), ZERO);

        assertThat(result.excluded()).extracting(ScoringResult.Exclusion::breedId).contains("chihuahua", "akita");
        assertThat(result.recommendations()).allSatisfy(row -> {
            BreedFeatures breed = SeedCatalogue.breed(row.breedId());
            assertThat(breed.goodWithYoungChildren()).isGreaterThanOrEqualTo(3);
            assertThat(breed.goodWithOtherDogs()).isGreaterThanOrEqualTo(3);
        });
    }

    @Test
    void explicitSizeAndTraitLimitsAreNeverViolated() {
        ScoringResult result = recommend(profile(Map.of("allowed_sizes", List.of("Small"),
                "hard_limits", Map.of("coat_grooming_frequency", 2, "energy_level", 3))), ZERO);

        assertThat(result.recommendations()).isNotEmpty().allSatisfy(row -> {
            BreedFeatures breed = SeedCatalogue.breed(row.breedId());
            assertThat(breed.size().code()).isEqualTo("Small");
            assertThat(breed.coatGroomingFrequency()).isLessThanOrEqualTo(2);
            assertThat(breed.energyLevel()).isLessThanOrEqualTo(3);
        });
    }

    @Test
    void impossibleConstraintsReturnEmptyInsteadOfPadding() {
        ScoringResult result = recommend(profile(Map.of("hard_limits", Map.of("energy_level", 1))),
                RecommendationPolicy.MIN_SCORE);

        assertThat(result.recommendations()).isEmpty();
        assertThat(result.eligibleBreeds()).isZero();
        assertThat(result.excluded()).hasSize(30);
    }

    @Test
    void fewerThanThreeIsNotPadded() {
        ScoringResult result = recommend(profile(Map.of("allowed_sizes", List.of("Small"), "hard_limits",
                Map.of("energy_level", 3, "coat_grooming_frequency", 1, "barking_level", 1))),
                RecommendationPolicy.MIN_SCORE);

        assertThat(result.recommendations()).extracting(ScoredBreed::breedId).containsExactly("french-bulldog");
        assertThat(result.notice()).contains("лише 1");
    }

    @Test
    void relaxingACapacityNeverLowersTheSameBreedsScore() {
        for (String field : List.of("shedding_tolerance", "grooming_capacity", "mental_stimulation_capacity",
                "barking_tolerance")) {
            ScoringProfile low = profile(Map.of(field, 1));
            ScoringProfile high = profile(Map.of(field, 5));
            for (BreedFeatures breed : catalogue) {
                ScoredBreed a = engine.assess(low, breed);
                ScoredBreed b = engine.assess(high, breed);
                if (a.eligible()) {
                    assertThat(b.compatibilityIndex()).as(field + " / " + breed.breedId())
                            .isGreaterThanOrEqualTo(a.compatibilityIndex());
                }
            }
        }
    }

    @Test
    void catalogueOrderDoesNotChangeTheResult() {
        List<BreedFeatures> reversed = new ArrayList<>(catalogue);
        Collections.reverse(reversed);
        ScoringProfile profile = profile(Map.of("has_other_dogs", true, "has_cats", true));

        assertThat(engine.recommend(profile, reversed, RecommendationPolicy.LIMIT, RecommendationPolicy.MIN_SCORE))
                .isEqualTo(recommend(profile, RecommendationPolicy.MIN_SCORE));
    }

    @Test
    void excludedBreedIsVisibleButNotScoredInComparison() {
        ComparisonResult result = engine.compare(profile(Map.of()),
                List.of(SeedCatalogue.breed("chihuahua"), SeedCatalogue.breed("cavalier-king-charles-spaniel")));

        ScoredBreed chihuahua = result.breeds().getFirst();
        assertThat(chihuahua.eligible()).isFalse();
        assertThat(chihuahua.compatibilityIndex()).isNull();
        assertThat(chihuahua.exclusionReasons()).isNotEmpty();
        assertThat(result.breeds().get(1).compatibilityIndex()).isNotNull();
    }

    @Test
    void missingBreedDataForCatsIsReported() {
        ScoringResult result = recommend(profile(Map.of("has_cats", true)), RecommendationPolicy.MIN_SCORE);

        assertThat(result.unassessedCriteria()).anyMatch(text -> text.contains("котами"));
        assertThat(result.recommendations())
                .allSatisfy(row -> assertThat(row.warnings()).anyMatch(text -> text.contains("котами")));
    }

    @Test
    void unsupportedTimeAnswersDoNotCreateInventedBreedPenalties() {
        ScoringResult none = recommend(profile(Map.of("alone_hours_daily", 0)), RecommendationPolicy.MIN_SCORE);
        ScoringResult twelve = recommend(profile(Map.of("alone_hours_daily", 12)), RecommendationPolicy.MIN_SCORE);

        assertThat(twelve.recommendations()).isEqualTo(none.recommendations());
        assertThat(twelve.unassessedCriteria()).isNotEmpty();
    }

    /** Replaces the reference's synthetic-scenario checks with invariants over many seeded random profiles. */
    @Test
    void invariantsHoldForRandomProfiles() {
        Random random = new Random(20261122);
        for (int i = 0; i < 2000; i++) {
            ScoringProfile profile = randomProfile(random);
            ScoringResult result = recommend(profile, RecommendationPolicy.MIN_SCORE);

            assertThat(result.recommendations()).hasSizeLessThanOrEqualTo(RecommendationPolicy.LIMIT);
            assertThat(indexes(result)).isSortedAccordingTo(Collections.reverseOrder())
                    .allSatisfy(index -> assertThat(index).isGreaterThanOrEqualTo(RecommendationPolicy.MIN_SCORE));
            assertThat(result.eligibleBreeds() + result.excluded().size()).isEqualTo(catalogue.size());
            assertThat(result.notice()).isNotBlank();
            for (ScoredBreed row : result.recommendations()) {
                BreedFeatures breed = SeedCatalogue.breed(row.breedId());
                assertThat(profile.allowedSizes().isEmpty() || profile.allowedSizes().contains(breed.size())).isTrue();
                profile.hardLimits().forEach((limit, max) ->
                        assertThat(breed.rating(limit)).as(limit + " limit").isLessThanOrEqualTo(max));
                if (profile.hasYoungChildren()) {
                    assertThat(breed.goodWithYoungChildren()).isGreaterThanOrEqualTo(3);
                }
                if (profile.hasOtherDogs()) {
                    assertThat(breed.goodWithOtherDogs()).isGreaterThanOrEqualTo(3);
                }
            }
        }
    }

    private ScoringResult recommend(ScoringProfile profile, BigDecimal minScore) {
        return engine.recommend(profile, catalogue, RecommendationPolicy.LIMIT, minScore);
    }

    private static List<BigDecimal> indexes(ScoringResult result) {
        return result.recommendations().stream().map(ScoredBreed::compatibilityIndex).toList();
    }

    /** The example profile (baseline team's example_profile.json) with changes applied. */
    private static ScoringProfile profile(Map<String, Object> changes) {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.putAll(changes);
        return ScoringInputs.profile(AnswersParser.parse(raw));
    }

    private static ScoringProfile randomProfile(Random random) {
        Map<String, Object> raw = TestAnswers.validRaw();
        for (String field : List.of("activity_level", "shedding_tolerance", "grooming_capacity",
                "mental_stimulation_capacity", "barking_tolerance")) {
            raw.put(field, 1 + random.nextInt(5));
        }
        List<String> sizes = List.of("XSmall", "Small", "Medium", "Large", "XLarge");
        raw.put("preferred_size", random.nextInt(6) == 5 ? "any" : sizes.get(random.nextInt(5)));
        raw.put("has_young_children", random.nextBoolean());
        raw.put("has_other_dogs", random.nextBoolean());
        raw.put("has_cats", random.nextBoolean());
        if (random.nextInt(3) == 0) {
            List<String> shuffled = new ArrayList<>(sizes);
            Collections.shuffle(shuffled, random);
            raw.put("allowed_sizes", shuffled.subList(0, 1 + random.nextInt(4)));
        }
        if (random.nextInt(3) == 0) {
            Set<String> criteria = Arrays.stream(HardLimit.values()).map(HardLimit::code)
                    .filter(code -> random.nextBoolean()).collect(Collectors.toSet());
            raw.put("hard_limits", criteria.stream().collect(Collectors.toMap(c -> c, c -> 2 + random.nextInt(4))));
        }
        return ScoringInputs.profile(AnswersParser.parse(raw));
    }
}
