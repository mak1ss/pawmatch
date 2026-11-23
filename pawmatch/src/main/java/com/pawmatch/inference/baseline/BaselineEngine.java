package com.pawmatch.inference.baseline;

import static com.pawmatch.inference.baseline.BaselineTexts.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.pawmatch.inference.engine.ModelEngine;
import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.HardLimit;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;
import com.pawmatch.recommendation.domain.ScoringResult.Exclusion;
import com.pawmatch.recommendation.domain.ScoringResult.Factor;
import com.pawmatch.recommendation.domain.ScoringResult.ScoredBreed;
import com.pawmatch.recommendation.domain.SizeCategory;
import org.springframework.stereotype.Component;

/**
 * Java port of the ML team's weighted-rules baseline {@code pawmatch-baseline-1.0.0} ({@code pawmatch.py}).
 * <p>
 * Behaviour, texts and arithmetic mirror the Python reference exactly; {@code BaselineParityTest} compares the two on
 * generated golden cases (see {@code tools/baseline-parity}). Floating-point steps follow Python's order of
 * operations, and rounding uses the exact binary value, half-even, like Python's {@code round()}.
 * <p>
 * Agreed differences from the reference output: no per-breed provenance fields, and {@code excluded} is sorted by
 * breed id.
 */
@Component
public class BaselineEngine implements ModelEngine {

    public static final String ID = "baseline";

    private static final double W_ACTIVITY = 3.0;
    private static final double W_SHEDDING = 2.0;
    private static final double W_GROOMING = 2.0;
    private static final double W_MENTAL_STIMULATION = 2.0;
    private static final double W_BARKING = 1.0;
    private static final double W_SIZE = 1.0;
    private static final double W_YOUNG_CHILDREN = 1.5;
    private static final double W_OTHER_DOGS = 1.5;
    private static final int COMPATIBILITY_THRESHOLD = 3;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ScoringResult recommend(ScoringProfile profile, List<BreedFeatures> breeds, int limit,
                                   BigDecimal minScore) {
        List<ScoredBreed> rows = breeds.stream().map(breed -> assess(profile, breed)).toList();
        List<ScoredBreed> eligible = rows.stream().filter(ScoredBreed::eligible).toList();
        List<ScoredBreed> above = eligible.stream()
                .filter(row -> row.compatibilityIndex().compareTo(minScore) >= 0)
                .sorted(Comparator.comparing(ScoredBreed::compatibilityIndex).reversed()
                        .thenComparing(ScoredBreed::breedId))
                .toList();
        List<ScoredBreed> results = above.subList(0, Math.min(limit, above.size()));

        List<String> unassessed = new ArrayList<>(UNASSESSED);
        if (profile.hasCats()) {
            unassessed.add(UNASSESSED_CATS);
        }
        List<Exclusion> excluded = rows.stream()
                .filter(row -> !row.eligible())
                .map(row -> new Exclusion(row.breedId(), row.exclusionReasons()))
                .sorted(Comparator.comparing(Exclusion::breedId))
                .toList();
        return new ScoringResult(MODEL_VERSION, MODEL_TYPE, SCORE_MEANING, minScore, rows.size(), eligible.size(),
                above.size(), results, results.size() < 3 ? noticeFewer(results.size()) : NOTICE_DEFAULT,
                unassessed, excluded);
    }

    @Override
    public ComparisonResult compare(ScoringProfile profile, List<BreedFeatures> breeds) {
        return new ComparisonResult(MODEL_VERSION, breeds.stream().map(breed -> assess(profile, breed)).toList());
    }

    ScoredBreed assess(ScoringProfile profile, BreedFeatures breed) {
        List<String> excluded = new ArrayList<>();
        if (!profile.allowedSizes().isEmpty() && !profile.allowedSizes().contains(breed.size())) {
            excluded.add(EXCLUDED_SIZE);
        }
        for (Map.Entry<HardLimit, Integer> limit : profile.hardLimits().entrySet()) {
            int rating = breed.rating(limit.getKey());
            if (rating > limit.getValue()) {
                excluded.add(excludedHardLimit(limit.getKey().code(), rating, limit.getValue()));
            }
        }
        // Conservative prototype policy from the reference, not a guarantee of safety.
        if (profile.hasYoungChildren() && breed.goodWithYoungChildren() < COMPATIBILITY_THRESHOLD) {
            excluded.add(EXCLUDED_YOUNG_CHILDREN);
        }
        if (profile.hasOtherDogs() && breed.goodWithOtherDogs() < COMPATIBILITY_THRESHOLD) {
            excluded.add(EXCLUDED_OTHER_DOGS);
        }

        List<Factor> factors = new ArrayList<>();
        int energy = breed.energyLevel();
        int activity = profile.activityLevel();
        factors.add(factor("activity", LABEL_ACTIVITY, 1 - Math.abs(energy - activity) / 4.0, energy, activity,
                W_ACTIVITY, BaselineTexts.activity(energy, activity)));
        factors.add(capacityFactor("shedding", LABEL_SHEDDING, breed.sheddingLevel(), profile.sheddingTolerance(),
                W_SHEDDING));
        factors.add(capacityFactor("grooming", LABEL_GROOMING, breed.coatGroomingFrequency(),
                profile.groomingCapacity(), W_GROOMING));
        factors.add(capacityFactor("mental_stimulation", LABEL_MENTAL_STIMULATION, breed.mentalStimulationNeeds(),
                profile.mentalStimulationCapacity(), W_MENTAL_STIMULATION));
        factors.add(capacityFactor("barking", LABEL_BARKING, breed.barkingLevel(), profile.barkingTolerance(),
                W_BARKING));
        if (!profile.anySize()) {
            SizeCategory size = breed.size();
            SizeCategory wanted = profile.preferredSize();
            factors.add(factor("size", LABEL_SIZE, 1 - Math.abs(size.ordinal() - wanted.ordinal()) / 4.0,
                    size.code(), wanted.code(), W_SIZE, BaselineTexts.size(size.code(), wanted.code())));
        }
        if (profile.hasYoungChildren()) {
            int observed = breed.goodWithYoungChildren();
            factors.add(factor("young_children", LABEL_YOUNG_CHILDREN, (observed - 1) / 4.0, observed, true,
                    W_YOUNG_CHILDREN, compatibility(LABEL_YOUNG_CHILDREN, observed)));
        }
        if (profile.hasOtherDogs()) {
            int observed = breed.goodWithOtherDogs();
            factors.add(factor("other_dogs", LABEL_OTHER_DOGS, (observed - 1) / 4.0, observed, true,
                    W_OTHER_DOGS, compatibility(LABEL_OTHER_DOGS, observed)));
        }

        // Same order of operations as the reference: sum of weights, then 100 * sum(w * match) / total.
        double totalWeight = 0;
        double weighted = 0;
        for (Factor f : factors) {
            totalWeight += f.weight();
        }
        for (Factor f : factors) {
            weighted += f.weight() * f.match().doubleValue();
        }
        BigDecimal score = round(100 * weighted / totalWeight, 1);

        List<String> warnings = new ArrayList<>(factors.stream()
                .filter(f -> f.match().compareTo(BigDecimal.ONE) < 0)
                .sorted(Comparator.comparing(Factor::match).thenComparing(Factor::criterion))
                .map(Factor::explanation)
                .toList());
        warnings.add(WARNING_TENDENCIES);
        if (profile.hasYoungChildren()) {
            warnings.add(WARNING_CHILDREN_SUPERVISION);
        }
        if (profile.hasCats()) {
            warnings.add(WARNING_CATS);
        }
        List<String> reasons = factors.stream()
                .filter(f -> f.match().compareTo(BigDecimal.ONE) == 0)
                .map(Factor::explanation)
                .toList();
        boolean eligible = excluded.isEmpty();
        return new ScoredBreed(breed.breedId(), breed.breedName(), breed.breedNameEn(), eligible, excluded,
                eligible ? score : null, factors, reasons, warnings, breed.sourceUrl());
    }

    private static Factor capacityFactor(String criterion, String label, int observed, int capacity, double weight) {
        return factor(criterion, label, 1 - Math.max(0, observed - capacity) / 4.0, observed, capacity, weight,
                BaselineTexts.capacity(label, observed, capacity));
    }

    private static Factor factor(String criterion, String label, double match, Object breedValue,
                                 Object profileValue, double weight, String explanation) {
        return new Factor(criterion, label, breedValue, profileValue, round(match, 4), weight, explanation);
    }

    /** Python's round(x, n): the exact binary value of x, rounded half-to-even. */
    static BigDecimal round(double value, int digits) {
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN);
    }
}
