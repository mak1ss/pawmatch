package com.pawmatch.questionnaire.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A complete, valid set of questionnaire answers. Build it with {@link AnswersParser#parse}, which reports every
 * problem as a field error; the constructor only guards invariants.
 *
 * @param activityLevel             desired activity, 1–5
 * @param sheddingTolerance         acceptable shedding, 1–5 (5 = heavy shedding is fine)
 * @param groomingCapacity          coat-care capacity, 1–5
 * @param mentalStimulationCapacity readiness for mental exercise and training, 1–5
 * @param barkingTolerance          acceptable barking, 1–5
 * @param preferredSize             soft size preference
 * @param walkMinutesDaily          minutes per day for walks, 0–1440
 * @param aloneHoursDaily           hours per day the dog is alone, 0–24
 * @param trainingMinutesDaily      minutes per day for training, 0–1440
 * @param allowedSizes              hard size filter; empty means "not set" (all sizes allowed)
 * @param hardLimits                maximum allowed breed ratings (1–5) per criterion; empty means "no limits"
 */
public record Answers(
        int activityLevel,
        int sheddingTolerance,
        int groomingCapacity,
        int mentalStimulationCapacity,
        int barkingTolerance,
        PreferredSize preferredSize,
        boolean hasYoungChildren,
        boolean hasOtherDogs,
        boolean hasCats,
        HousingType housingType,
        double walkMinutesDaily,
        double aloneHoursDaily,
        OwnerExperience ownerExperience,
        double trainingMinutesDaily,
        List<DogSize> allowedSizes,
        Map<HardLimitCriterion, Integer> hardLimits) {

    public Answers {
        Objects.requireNonNull(preferredSize, "preferredSize");
        Objects.requireNonNull(housingType, "housingType");
        Objects.requireNonNull(ownerExperience, "ownerExperience");
        allowedSizes = allowedSizes == null ? List.of() : List.copyOf(allowedSizes);
        hardLimits = hardLimits == null || hardLimits.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new EnumMap<>(hardLimits));
    }
}
