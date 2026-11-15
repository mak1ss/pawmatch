package com.pawmatch.questionnaire.domain;

/** The user's experience of keeping dogs. */
public enum OwnerExperience implements Coded {
    NONE("none"),
    SOME("some"),
    EXPERIENCED("experienced");

    private final String code;

    OwnerExperience(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
