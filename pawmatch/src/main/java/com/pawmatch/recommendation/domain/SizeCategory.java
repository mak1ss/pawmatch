package com.pawmatch.recommendation.domain;

import java.util.Arrays;

/** AKC size category as the scoring engine sees it, ordered smallest to largest (the order drives size distance). */
public enum SizeCategory {
    XSMALL("XSmall"), SMALL("Small"), MEDIUM("Medium"), LARGE("Large"), XLARGE("XLarge");

    private final String code;

    SizeCategory(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static SizeCategory fromCode(String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown size category: " + code));
    }
}
