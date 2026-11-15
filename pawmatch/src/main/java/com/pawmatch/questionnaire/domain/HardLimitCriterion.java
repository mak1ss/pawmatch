package com.pawmatch.questionnaire.domain;

/** Breed rating a user may cap with a hard limit (a breed above the limit is excluded). */
public enum HardLimitCriterion implements Coded {
    ENERGY_LEVEL("energy_level"),
    SHEDDING_LEVEL("shedding_level"),
    COAT_GROOMING_FREQUENCY("coat_grooming_frequency"),
    MENTAL_STIMULATION_NEEDS("mental_stimulation_needs"),
    BARKING_LEVEL("barking_level");

    private final String code;

    HardLimitCriterion(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
