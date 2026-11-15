package com.pawmatch.questionnaire.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** An enum with a stable external code, as used in the questionnaire JSON contract. */
public interface Coded {

    String code();

    static <E extends Enum<E> & Coded> Optional<E> fromCode(Class<E> type, Object code) {
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.code().equals(code)).findFirst();
    }

    static <E extends Enum<E> & Coded> List<String> codes(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Coded::code).toList();
    }
}
