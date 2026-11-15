package com.pawmatch.questionnaire.domain;

/** AKC size category as used in questionnaire answers, ordered from smallest to largest. */
public enum DogSize implements Coded {
    XSMALL("XSmall"),
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
    XLARGE("XLarge");

    private final String code;

    DogSize(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
