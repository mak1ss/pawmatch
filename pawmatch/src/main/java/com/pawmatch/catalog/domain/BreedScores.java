package com.pawmatch.catalog.domain;

/**
 * The 14 AKC ordinal ratings of a breed. Every rating is an integer from 1 to 5.
 * They describe breed tendencies, not a specific dog.
 */
public record BreedScores(
        int energyLevel,
        int coatGroomingFrequency,
        int sheddingLevel,
        int trainabilityLevel,
        int goodWithYoungChildren,
        int goodWithOtherDogs,
        int mentalStimulationNeeds,
        int barkingLevel,
        int adaptabilityLevel,
        int affectionateWithFamily,
        int opennessToStrangers,
        int playfulnessLevel,
        int droolingLevel,
        int watchdogProtectiveNature) {

    public static final int MIN = 1;
    public static final int MAX = 5;

    public BreedScores {
        requireScore("energy_level", energyLevel);
        requireScore("coat_grooming_frequency", coatGroomingFrequency);
        requireScore("shedding_level", sheddingLevel);
        requireScore("trainability_level", trainabilityLevel);
        requireScore("good_with_young_children", goodWithYoungChildren);
        requireScore("good_with_other_dogs", goodWithOtherDogs);
        requireScore("mental_stimulation_needs", mentalStimulationNeeds);
        requireScore("barking_level", barkingLevel);
        requireScore("adaptability_level", adaptabilityLevel);
        requireScore("affectionate_with_family", affectionateWithFamily);
        requireScore("openness_to_strangers", opennessToStrangers);
        requireScore("playfulness_level", playfulnessLevel);
        requireScore("drooling_level", droolingLevel);
        requireScore("watchdogprotective_nature", watchdogProtectiveNature);
    }

    /** Validates an optional 1–5 rating; {@code null} means unknown. */
    public static Integer requireOptionalScore(String name, Integer value) {
        if (value != null) {
            requireScore(name, value);
        }
        return value;
    }

    static void requireScore(String name, int value) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(name + " must be an integer 1–5, got: " + value);
        }
    }
}
