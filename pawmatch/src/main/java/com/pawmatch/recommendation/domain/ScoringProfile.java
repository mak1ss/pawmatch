package com.pawmatch.recommendation.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The scoring engine's view of a user: the questionnaire answers it needs, independent of how the questionnaire
 * module stores them. This is the input contract every engine (baseline, future ML) receives.
 *
 * @param preferredSize soft size preference; {@code null} means "any"
 * @param allowedSizes  hard size filter; empty means no filter
 * @param hardLimits    maximum allowed breed ratings; empty means no limits
 * @param housingType   collected but not scored by the baseline (reported as unassessed)
 * @param ownerExperience collected but not scored by the baseline (reported as unassessed)
 */
public record ScoringProfile(
        int activityLevel,
        int sheddingTolerance,
        int groomingCapacity,
        int mentalStimulationCapacity,
        int barkingTolerance,
        SizeCategory preferredSize,
        boolean hasYoungChildren,
        boolean hasOtherDogs,
        boolean hasCats,
        String housingType,
        double walkMinutesDaily,
        double aloneHoursDaily,
        String ownerExperience,
        double trainingMinutesDaily,
        Set<SizeCategory> allowedSizes,
        Map<HardLimit, Integer> hardLimits) {

    public ScoringProfile {
        Objects.requireNonNull(housingType, "housingType");
        Objects.requireNonNull(ownerExperience, "ownerExperience");
        allowedSizes = allowedSizes == null || allowedSizes.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(allowedSizes));
        hardLimits = hardLimits == null || hardLimits.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new EnumMap<>(hardLimits));
    }

    public boolean anySize() {
        return preferredSize == null;
    }
}
