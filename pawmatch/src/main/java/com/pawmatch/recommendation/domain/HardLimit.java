package com.pawmatch.recommendation.domain;

/** Breed rating a profile can cap with a hard limit; a breed above the limit is excluded. Declaration order is the canonical order. */
public enum HardLimit {
    ENERGY_LEVEL("energy_level"),
    SHEDDING_LEVEL("shedding_level"),
    COAT_GROOMING_FREQUENCY("coat_grooming_frequency"),
    MENTAL_STIMULATION_NEEDS("mental_stimulation_needs"),
    BARKING_LEVEL("barking_level");

    private final String code;

    HardLimit(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
