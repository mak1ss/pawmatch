package com.pawmatch.questionnaire.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.pawmatch.common.kernel.FieldError;
import com.pawmatch.common.kernel.RequestValidationException;

/**
 * Converts raw JSON-shaped values (maps, lists, strings, booleans, numbers) into {@link Answers} and back.
 * <p>
 * Mirrors the team baseline's {@code validate_profile}: strict types (no {@code 1} for {@code true}, no {@code "3"} or
 * {@code 3.0} for an integer), unknown fields rejected, and <b>all</b> errors collected before failing.
 * Every error has a stable {@link ErrorCode} with parameters for the client, plus a Ukrainian fallback message.
 * Field names are the snake_case API names. Ranges are shared with {@link QuestionnaireContract}.
 */
public final class AnswersParser {

    public static final String ACTIVITY_LEVEL = "activity_level";
    public static final String SHEDDING_TOLERANCE = "shedding_tolerance";
    public static final String GROOMING_CAPACITY = "grooming_capacity";
    public static final String MENTAL_STIMULATION_CAPACITY = "mental_stimulation_capacity";
    public static final String BARKING_TOLERANCE = "barking_tolerance";
    public static final String PREFERRED_SIZE = "preferred_size";
    public static final String HAS_YOUNG_CHILDREN = "has_young_children";
    public static final String HAS_OTHER_DOGS = "has_other_dogs";
    public static final String HAS_CATS = "has_cats";
    public static final String HOUSING_TYPE = "housing_type";
    public static final String WALK_MINUTES_DAILY = "walk_minutes_daily";
    public static final String ALONE_HOURS_DAILY = "alone_hours_daily";
    public static final String OWNER_EXPERIENCE = "owner_experience";
    public static final String TRAINING_MINUTES_DAILY = "training_minutes_daily";
    public static final String ALLOWED_SIZES = "allowed_sizes";
    public static final String HARD_LIMITS = "hard_limits";

    /** The 14 required answers. */
    public static final Set<String> REQUIRED = Set.of(
            ACTIVITY_LEVEL, SHEDDING_TOLERANCE, GROOMING_CAPACITY, MENTAL_STIMULATION_CAPACITY, BARKING_TOLERANCE,
            PREFERRED_SIZE, HAS_YOUNG_CHILDREN, HAS_OTHER_DOGS, HAS_CATS, HOUSING_TYPE, WALK_MINUTES_DAILY,
            ALONE_HOURS_DAILY, OWNER_EXPERIENCE, TRAINING_MINUTES_DAILY);
    /** Optional hard constraints. */
    public static final Set<String> OPTIONAL = Set.of(ALLOWED_SIZES, HARD_LIMITS);

    public static final int SCALE_MIN = 1;
    public static final int SCALE_MAX = 5;
    public static final int MINUTES_MAX = 1440;
    public static final int HOURS_MAX = 24;

    /** Stable error codes returned in {@code field_errors[].code}. */
    public enum ErrorCode implements Coded {
        /** A required field is missing. */
        REQUIRED("required"),
        /** The field isn't part of the questionnaire. */
        UNKNOWN_FIELD("unknown_field"),
        /** Not an integer, or outside {@code min}–{@code max}. */
        INVALID_INTEGER("invalid_integer"),
        /** Not a number, or outside {@code min}–{@code max}. */
        INVALID_NUMBER("invalid_number"),
        /** Not {@code true} / {@code false}. */
        INVALID_BOOLEAN("invalid_boolean"),
        /** Not one of {@code allowed}. */
        INVALID_CHOICE("invalid_choice"),
        /** Not a non-empty list of values from {@code allowed}. */
        INVALID_LIST("invalid_list"),
        /** A list contains the same value twice. */
        DUPLICATE_VALUE("duplicate_value"),
        /** Not a JSON object. */
        INVALID_OBJECT("invalid_object"),
        /** An object key that isn't one of {@code allowed}. */
        UNSUPPORTED_KEY("unsupported_key");

        private final String code;

        ErrorCode(String code) {
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }
    }

    private AnswersParser() {
    }

    /**
     * @throws RequestValidationException with every invalid field, sorted by field name
     */
    public static Answers parse(Map<String, ?> raw) {
        if (raw == null) {
            throw new RequestValidationException(List.of(new FieldError("answers", ErrorCode.INVALID_OBJECT.code(),
                    "Очікується JSON-об’єкт анкети.")));
        }
        Errors errors = new Errors();
        for (String key : new TreeSet<>(REQUIRED)) {
            if (!raw.containsKey(key)) {
                errors.add(key, ErrorCode.REQUIRED, "Обов’язкове поле.", null);
            }
        }
        for (String key : new TreeSet<>(raw.keySet())) {
            if (!REQUIRED.contains(key) && !OPTIONAL.contains(key)) {
                errors.add(key, ErrorCode.UNKNOWN_FIELD, "Невідоме поле.", null);
            }
        }

        int activity = scale(raw, ACTIVITY_LEVEL, errors);
        int shedding = scale(raw, SHEDDING_TOLERANCE, errors);
        int grooming = scale(raw, GROOMING_CAPACITY, errors);
        int mental = scale(raw, MENTAL_STIMULATION_CAPACITY, errors);
        int barking = scale(raw, BARKING_TOLERANCE, errors);
        PreferredSize preferredSize = choice(raw, PREFERRED_SIZE, PreferredSize.class, errors);
        boolean youngChildren = bool(raw, HAS_YOUNG_CHILDREN, errors);
        boolean otherDogs = bool(raw, HAS_OTHER_DOGS, errors);
        boolean cats = bool(raw, HAS_CATS, errors);
        HousingType housing = choice(raw, HOUSING_TYPE, HousingType.class, errors);
        double walk = number(raw, WALK_MINUTES_DAILY, MINUTES_MAX, errors);
        double alone = number(raw, ALONE_HOURS_DAILY, HOURS_MAX, errors);
        OwnerExperience experience = choice(raw, OWNER_EXPERIENCE, OwnerExperience.class, errors);
        double training = number(raw, TRAINING_MINUTES_DAILY, MINUTES_MAX, errors);
        List<DogSize> allowedSizes = allowedSizes(raw, errors);
        Map<HardLimitCriterion, Integer> hardLimits = hardLimits(raw, errors);

        errors.throwIfAny();
        return new Answers(activity, shedding, grooming, mental, barking, preferredSize, youngChildren, otherDogs, cats,
                housing, walk, alone, experience, training, allowedSizes, hardLimits);
    }

    /** Canonical JSON-shaped form of answers (snake_case keys), the inverse of {@link #parse}. */
    public static Map<String, Object> toMap(Answers answers) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ACTIVITY_LEVEL, answers.activityLevel());
        map.put(SHEDDING_TOLERANCE, answers.sheddingTolerance());
        map.put(GROOMING_CAPACITY, answers.groomingCapacity());
        map.put(MENTAL_STIMULATION_CAPACITY, answers.mentalStimulationCapacity());
        map.put(BARKING_TOLERANCE, answers.barkingTolerance());
        map.put(PREFERRED_SIZE, answers.preferredSize().code());
        map.put(HAS_YOUNG_CHILDREN, answers.hasYoungChildren());
        map.put(HAS_OTHER_DOGS, answers.hasOtherDogs());
        map.put(HAS_CATS, answers.hasCats());
        map.put(HOUSING_TYPE, answers.housingType().code());
        map.put(WALK_MINUTES_DAILY, compactNumber(answers.walkMinutesDaily()));
        map.put(ALONE_HOURS_DAILY, compactNumber(answers.aloneHoursDaily()));
        map.put(OWNER_EXPERIENCE, answers.ownerExperience().code());
        map.put(TRAINING_MINUTES_DAILY, compactNumber(answers.trainingMinutesDaily()));
        if (!answers.allowedSizes().isEmpty()) {
            map.put(ALLOWED_SIZES, answers.allowedSizes().stream().map(DogSize::code).toList());
        }
        if (!answers.hardLimits().isEmpty()) {
            Map<String, Object> limits = new LinkedHashMap<>();
            answers.hardLimits().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> limits.put(e.getKey().code(), e.getValue()));
            map.put(HARD_LIMITS, limits);
        }
        return map;
    }

    private static int scale(Map<String, ?> raw, String key, Errors errors) {
        if (!raw.containsKey(key)) {
            return 0;
        }
        Long value = integral(raw.get(key));
        if (value == null || value < SCALE_MIN || value > SCALE_MAX) {
            errors.invalidScale(key);
            return 0;
        }
        return value.intValue();
    }

    private static boolean bool(Map<String, ?> raw, String key, Errors errors) {
        if (!raw.containsKey(key)) {
            return false;
        }
        if (raw.get(key) instanceof Boolean value) {
            return value;
        }
        errors.add(key, ErrorCode.INVALID_BOOLEAN, "Потрібне значення true або false.", null);
        return false;
    }

    private static <E extends Enum<E> & Coded> E choice(Map<String, ?> raw, String key, Class<E> type, Errors errors) {
        if (!raw.containsKey(key)) {
            return null;
        }
        Object value = raw.get(key);
        if (value instanceof String) {
            Optional<E> match = Coded.fromCode(type, value);
            if (match.isPresent()) {
                return match.get();
            }
        }
        List<String> allowed = Coded.codes(type);
        errors.add(key, ErrorCode.INVALID_CHOICE, "Допустимі значення: " + String.join(", ", allowed) + ".",
                Map.of("allowed", allowed));
        return null;
    }

    private static double number(Map<String, ?> raw, String key, int max, Errors errors) {
        if (!raw.containsKey(key)) {
            return 0;
        }
        Object value = raw.get(key);
        Double number = null;
        if (integral(value) != null) {
            number = integral(value).doubleValue();
        } else if (value instanceof Double || value instanceof Float) {
            number = ((Number) value).doubleValue();
        } else if (value instanceof BigDecimal decimal) {
            number = decimal.doubleValue();
        }
        if (number == null || !Double.isFinite(number) || number < 0 || number > max) {
            errors.add(key, ErrorCode.INVALID_NUMBER, "Потрібне число від 0 до " + max + ".",
                    Map.of("min", 0, "max", max));
            return 0;
        }
        return number;
    }

    private static List<DogSize> allowedSizes(Map<String, ?> raw, Errors errors) {
        if (!raw.containsKey(ALLOWED_SIZES)) {
            return List.of();
        }
        List<String> allowed = Coded.codes(DogSize.class);
        String invalidMessage = "Потрібен непорожній список розмірів: " + String.join(", ", allowed) + ".";
        if (!(raw.get(ALLOWED_SIZES) instanceof List<?> list) || list.isEmpty()) {
            errors.add(ALLOWED_SIZES, ErrorCode.INVALID_LIST, invalidMessage, Map.of("allowed", allowed));
            return List.of();
        }
        List<DogSize> sizes = new ArrayList<>();
        for (Object item : list) {
            Optional<DogSize> size = item instanceof String ? Coded.fromCode(DogSize.class, item) : Optional.empty();
            if (size.isEmpty()) {
                errors.add(ALLOWED_SIZES, ErrorCode.INVALID_LIST, invalidMessage, Map.of("allowed", allowed));
                return List.of();
            }
            sizes.add(size.get());
        }
        if (new HashSet<>(sizes).size() != sizes.size()) {
            errors.add(ALLOWED_SIZES, ErrorCode.DUPLICATE_VALUE, "Розміри не повинні повторюватися.", null);
            return List.of();
        }
        sizes.sort(Comparator.naturalOrder());
        return sizes;
    }

    private static Map<HardLimitCriterion, Integer> hardLimits(Map<String, ?> raw, Errors errors) {
        if (!raw.containsKey(HARD_LIMITS)) {
            return Map.of();
        }
        if (!(raw.get(HARD_LIMITS) instanceof Map<?, ?> limits)) {
            errors.add(HARD_LIMITS, ErrorCode.INVALID_OBJECT, "Потрібен об’єкт з максимальними оцінками.", null);
            return Map.of();
        }
        Map<HardLimitCriterion, Integer> result = new EnumMap<>(HardLimitCriterion.class);
        for (var entry : limits.entrySet()) {
            String field = HARD_LIMITS + "." + entry.getKey();
            Optional<HardLimitCriterion> criterion = Coded.fromCode(HardLimitCriterion.class, entry.getKey());
            if (criterion.isEmpty()) {
                errors.add(field, ErrorCode.UNSUPPORTED_KEY, "Непідтримуваний критерій.",
                        Map.of("allowed", Coded.codes(HardLimitCriterion.class)));
                continue;
            }
            Long value = integral(entry.getValue());
            if (value == null || value < SCALE_MIN || value > SCALE_MAX) {
                errors.invalidScale(field);
                continue;
            }
            result.put(criterion.get(), value.intValue());
        }
        return result;
    }

    /** The value as a long if it is a JSON integer (not a boolean, string or decimal); otherwise null. */
    private static Long integral(Object value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return ((Number) value).longValue();
        }
        if (value instanceof BigInteger big && big.bitLength() < 64) {
            return big.longValue();
        }
        if (value instanceof BigInteger) {
            return Long.MAX_VALUE; // out of every allowed range
        }
        return null;
    }

    private static Number compactNumber(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? (Number) (long) value : (Number) value;
    }

    private static final class Errors {
        private final List<FieldError> list = new ArrayList<>();

        void add(String field, ErrorCode code, String message, Map<String, Object> params) {
            list.add(new FieldError(field, code.code(), message, params));
        }

        void invalidScale(String field) {
            add(field, ErrorCode.INVALID_INTEGER, "Потрібне ціле число від " + SCALE_MIN + " до " + SCALE_MAX + ".",
                    Map.of("min", SCALE_MIN, "max", SCALE_MAX));
        }

        void throwIfAny() {
            if (!list.isEmpty()) {
                list.sort(Comparator.comparing(FieldError::field));
                throw new RequestValidationException(list);
            }
        }
    }
}
