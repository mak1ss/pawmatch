package com.pawmatch.recommendation.application.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.questionnaire.domain.Answers;
import com.pawmatch.questionnaire.domain.HardLimitCriterion;
import com.pawmatch.questionnaire.domain.PreferredSize;
import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.HardLimit;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.SizeCategory;

/**
 * Translates other modules' types into the scoring engine's inputs, so engines never depend on how the
 * questionnaire or the catalogue model their data. Mapping goes through the shared external codes.
 */
public final class ScoringInputs {

    private ScoringInputs() {
    }

    public static ScoringProfile profile(Answers answers) {
        Set<SizeCategory> allowed = EnumSet.noneOf(SizeCategory.class);
        answers.allowedSizes().forEach(size -> allowed.add(SizeCategory.fromCode(size.code())));
        Map<HardLimit, Integer> limits = new EnumMap<>(HardLimit.class);
        answers.hardLimits().forEach((criterion, max) -> limits.put(hardLimit(criterion), max));
        return new ScoringProfile(
                answers.activityLevel(), answers.sheddingTolerance(), answers.groomingCapacity(),
                answers.mentalStimulationCapacity(), answers.barkingTolerance(),
                answers.preferredSize() == PreferredSize.ANY ? null : SizeCategory.fromCode(answers.preferredSize().code()),
                answers.hasYoungChildren(), answers.hasOtherDogs(), answers.hasCats(),
                answers.housingType().code(), answers.walkMinutesDaily(), answers.aloneHoursDaily(),
                answers.ownerExperience().code(), answers.trainingMinutesDaily(), allowed, limits);
    }

    public static BreedFeatures features(Breed breed) {
        var s = breed.scores();
        return new BreedFeatures(breed.slug(), breed.nameUk(), breed.nameEn(), SizeCategory.fromCode(breed.size().code()),
                s.energyLevel(), s.sheddingLevel(), s.coatGroomingFrequency(), s.mentalStimulationNeeds(),
                s.barkingLevel(), s.goodWithYoungChildren(), s.goodWithOtherDogs(), breed.sourceUrl());
    }

    private static HardLimit hardLimit(HardLimitCriterion criterion) {
        for (HardLimit limit : HardLimit.values()) {
            if (limit.code().equals(criterion.code())) {
                return limit;
            }
        }
        throw new IllegalArgumentException("No scoring hard limit for " + criterion.code());
    }
}
