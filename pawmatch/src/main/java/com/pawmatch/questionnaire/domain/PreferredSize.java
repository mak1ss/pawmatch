package com.pawmatch.questionnaire.domain;

/** Soft size preference: one AKC size category, or {@code any} (no preference). */
public enum PreferredSize implements Coded {
    XSMALL("XSmall"),
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
    XLARGE("XLarge"),
    ANY("any");

    private final String code;

    PreferredSize(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
