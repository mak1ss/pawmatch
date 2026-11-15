package com.pawmatch.questionnaire.domain;

/** Where the user lives. */
public enum HousingType implements Coded {
    APARTMENT("apartment"),
    HOUSE("house"),
    OTHER("other");

    private final String code;

    HousingType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
