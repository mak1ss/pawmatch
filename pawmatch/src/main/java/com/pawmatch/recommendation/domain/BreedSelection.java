package com.pawmatch.recommendation.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.pawmatch.common.kernel.FieldError;
import com.pawmatch.common.kernel.RequestValidationException;

/**
 * The breeds a user wants to compare: 2–5 unique catalogue breed ids (slugs), in the order requested.
 *
 * @param breedIds breed slugs, the same {@code breed_id} the recommendations return
 */
public record BreedSelection(List<String> breedIds) {

    public static final String BREED_IDS = "breed_ids";

    /** Stable error codes returned in {@code field_errors[].code}. */
    public enum ErrorCode {
        /** {@code breed_ids} is missing. */
        REQUIRED("required"),
        /** A field other than {@code breed_ids}. */
        UNKNOWN_FIELD("unknown_field"),
        /** Not a list of strings. */
        INVALID_LIST("invalid_list"),
        /** Fewer than {@code min} or more than {@code max} breeds. */
        INVALID_COUNT("invalid_count"),
        /** The same breed more than once; {@code duplicates} lists them. */
        DUPLICATE_VALUE("duplicate_value"),
        /** Breed ids that aren't in the catalogue; {@code unknown} lists them. */
        UNKNOWN_BREED("unknown_breed");

        private final String code;

        ErrorCode(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    public BreedSelection {
        breedIds = List.copyOf(breedIds);
    }

    /**
     * @param raw            the JSON request body
     * @param knownBreedIds  slugs of every breed in the catalogue
     * @throws RequestValidationException with every problem found, never just the first
     */
    public static BreedSelection parse(Map<String, ?> raw, Set<String> knownBreedIds) {
        List<FieldError> errors = new ArrayList<>();
        Map<String, ?> body = raw == null ? Map.of() : raw;
        for (String key : new TreeSet<>(body.keySet())) {
            if (!BREED_IDS.equals(key)) {
                errors.add(error(key, ErrorCode.UNKNOWN_FIELD, "Невідоме поле.", null));
            }
        }

        List<String> ids = new ArrayList<>();
        if (!body.containsKey(BREED_IDS)) {
            errors.add(error(BREED_IDS, ErrorCode.REQUIRED, "Обов’язкове поле.", null));
        } else if (!(body.get(BREED_IDS) instanceof List<?> list) || !list.stream().allMatch(String.class::isInstance)) {
            errors.add(error(BREED_IDS, ErrorCode.INVALID_LIST, "Потрібен список ідентифікаторів порід.", null));
        } else {
            list.forEach(item -> ids.add((String) item));
            int min = RecommendationPolicy.COMPARE_MIN_BREEDS;
            int max = RecommendationPolicy.COMPARE_MAX_BREEDS;
            if (ids.size() < min || ids.size() > max) {
                errors.add(error(BREED_IDS, ErrorCode.INVALID_COUNT, "Для порівняння потрібно 2–5 порід.",
                        Map.of("min", min, "max", max)));
            }
            Set<String> seen = new LinkedHashSet<>();
            Set<String> duplicates = new LinkedHashSet<>();
            Set<String> unknown = new LinkedHashSet<>();
            for (String id : ids) {
                if (!seen.add(id)) {
                    duplicates.add(id);
                }
                if (!knownBreedIds.contains(id)) {
                    unknown.add(id);
                }
            }
            if (!duplicates.isEmpty()) {
                errors.add(error(BREED_IDS, ErrorCode.DUPLICATE_VALUE, "Породи не повинні повторюватися.",
                        Map.of("duplicates", List.copyOf(duplicates))));
            }
            if (!unknown.isEmpty()) {
                errors.add(error(BREED_IDS, ErrorCode.UNKNOWN_BREED, "Невідомі породи.",
                        Map.of("unknown", List.copyOf(unknown))));
            }
        }

        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
        return new BreedSelection(ids);
    }

    private static FieldError error(String field, ErrorCode code, String message, Map<String, Object> params) {
        return new FieldError(field, code.code(), message, params);
    }
}
