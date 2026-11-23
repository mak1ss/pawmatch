package com.pawmatch.recommendation.domain;

import java.util.Objects;

/**
 * What the scoring engine knows about a breed: identity, display names, size, the ratings it scores (1–5) and the
 * data source. Built from the catalogue by the recommendation service.
 *
 * @param breedId the breed slug, e.g. {@code labrador-retriever}
 */
public record BreedFeatures(
        String breedId,
        String breedName,
        String breedNameEn,
        SizeCategory size,
        int energyLevel,
        int sheddingLevel,
        int coatGroomingFrequency,
        int mentalStimulationNeeds,
        int barkingLevel,
        int goodWithYoungChildren,
        int goodWithOtherDogs,
        String sourceUrl) {

    public BreedFeatures {
        Objects.requireNonNull(breedId, "breedId");
        Objects.requireNonNull(size, "size");
    }

    /** The rating a hard limit applies to. */
    public int rating(HardLimit limit) {
        return switch (limit) {
            case ENERGY_LEVEL -> energyLevel;
            case SHEDDING_LEVEL -> sheddingLevel;
            case COAT_GROOMING_FREQUENCY -> coatGroomingFrequency;
            case MENTAL_STIMULATION_NEEDS -> mentalStimulationNeeds;
            case BARKING_LEVEL -> barkingLevel;
        };
    }
}
