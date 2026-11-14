package com.pawmatch.catalog.domain;

import java.util.Arrays;

/**
 * AKC size category, ordered from smallest to largest. The order matters for size-distance scoring.
 */
public enum Size {
    XSMALL("XSmall"),
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
    XLARGE("XLarge");

    private final String code;

    Size(String code) {
        this.code = code;
    }

    /** Catalogue and API representation, e.g. {@code XSmall}. */
    public String code() {
        return code;
    }

    public static Size fromCode(String code) {
        return Arrays.stream(values())
                .filter(size -> size.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "size must be one of XSmall, Small, Medium, Large, XLarge, got: '" + code + "'"));
    }
}
