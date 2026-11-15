package com.pawmatch.questionnaire.domain;

import static com.pawmatch.questionnaire.domain.AnswersParser.ACTIVITY_LEVEL;
import static com.pawmatch.questionnaire.domain.AnswersParser.ALLOWED_SIZES;
import static com.pawmatch.questionnaire.domain.AnswersParser.ALONE_HOURS_DAILY;
import static com.pawmatch.questionnaire.domain.AnswersParser.BARKING_TOLERANCE;
import static com.pawmatch.questionnaire.domain.AnswersParser.GROOMING_CAPACITY;
import static com.pawmatch.questionnaire.domain.AnswersParser.HARD_LIMITS;
import static com.pawmatch.questionnaire.domain.AnswersParser.HAS_CATS;
import static com.pawmatch.questionnaire.domain.AnswersParser.HAS_OTHER_DOGS;
import static com.pawmatch.questionnaire.domain.AnswersParser.HAS_YOUNG_CHILDREN;
import static com.pawmatch.questionnaire.domain.AnswersParser.HOURS_MAX;
import static com.pawmatch.questionnaire.domain.AnswersParser.HOUSING_TYPE;
import static com.pawmatch.questionnaire.domain.AnswersParser.MENTAL_STIMULATION_CAPACITY;
import static com.pawmatch.questionnaire.domain.AnswersParser.MINUTES_MAX;
import static com.pawmatch.questionnaire.domain.AnswersParser.OWNER_EXPERIENCE;
import static com.pawmatch.questionnaire.domain.AnswersParser.PREFERRED_SIZE;
import static com.pawmatch.questionnaire.domain.AnswersParser.SCALE_MAX;
import static com.pawmatch.questionnaire.domain.AnswersParser.SCALE_MIN;
import static com.pawmatch.questionnaire.domain.AnswersParser.SHEDDING_TOLERANCE;
import static com.pawmatch.questionnaire.domain.AnswersParser.TRAINING_MINUTES_DAILY;
import static com.pawmatch.questionnaire.domain.AnswersParser.WALK_MINUTES_DAILY;

import java.util.List;

/**
 * The machine-readable questionnaire contract: which answers exist, their types, ranges and allowed codes.
 * <p>
 * This is the backend's part of the questionnaire. Wording, labels, sections, ordering and help texts are UX copy
 * owned by the frontend, which maps them by field key and option code. Built from the same constants and enums as
 * {@link AnswersParser}, so the contract and the validation can't drift apart.
 * <p>
 * Bump {@link #VERSION} whenever a field, type, range or code changes; stored answers record the version they used.
 * Such a change is also a change to the scoring model's input, so agree it with the ML side first.
 */
public record QuestionnaireContract(String version, List<Field> fields) {

    public static final String VERSION = "1.0.0";

    /** Value shape of an answer. */
    public enum FieldType {
        /** Integer in [min, max]. */
        INTEGER,
        /** Number (decimals allowed) in [min, max]. */
        NUMBER,
        /** {@code true} or {@code false}. */
        BOOLEAN,
        /** One of {@code options}. */
        CHOICE,
        /** Non-empty list of distinct values from {@code options}. */
        MULTI_CHOICE,
        /** Object whose keys come from {@code options} and whose values are integers in [min, max]. */
        LIMITS
    }

    /**
     * @param min     lower bound for INTEGER, NUMBER and LIMITS values; null otherwise
     * @param max     upper bound for INTEGER, NUMBER and LIMITS values; null otherwise
     * @param options allowed codes for CHOICE and MULTI_CHOICE, allowed keys for LIMITS; empty otherwise
     */
    public record Field(String key, FieldType type, boolean required, Integer min, Integer max, List<String> options) {

        public Field {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    private static final QuestionnaireContract CURRENT = new QuestionnaireContract(VERSION, List.of(
            integer(ACTIVITY_LEVEL),
            integer(SHEDDING_TOLERANCE),
            integer(GROOMING_CAPACITY),
            integer(MENTAL_STIMULATION_CAPACITY),
            integer(BARKING_TOLERANCE),
            choice(PREFERRED_SIZE, Coded.codes(PreferredSize.class)),
            bool(HAS_YOUNG_CHILDREN),
            bool(HAS_OTHER_DOGS),
            bool(HAS_CATS),
            choice(HOUSING_TYPE, Coded.codes(HousingType.class)),
            number(WALK_MINUTES_DAILY, MINUTES_MAX),
            number(ALONE_HOURS_DAILY, HOURS_MAX),
            choice(OWNER_EXPERIENCE, Coded.codes(OwnerExperience.class)),
            number(TRAINING_MINUTES_DAILY, MINUTES_MAX),
            new Field(ALLOWED_SIZES, FieldType.MULTI_CHOICE, false, null, null, Coded.codes(DogSize.class)),
            new Field(HARD_LIMITS, FieldType.LIMITS, false, SCALE_MIN, SCALE_MAX, Coded.codes(HardLimitCriterion.class))));

    public QuestionnaireContract {
        fields = List.copyOf(fields);
    }

    public static QuestionnaireContract current() {
        return CURRENT;
    }

    private static Field integer(String key) {
        return new Field(key, FieldType.INTEGER, true, SCALE_MIN, SCALE_MAX, List.of());
    }

    private static Field number(String key, int max) {
        return new Field(key, FieldType.NUMBER, true, 0, max, List.of());
    }

    private static Field bool(String key) {
        return new Field(key, FieldType.BOOLEAN, true, null, null, List.of());
    }

    private static Field choice(String key, List<String> options) {
        return new Field(key, FieldType.CHOICE, true, null, null, options);
    }
}
