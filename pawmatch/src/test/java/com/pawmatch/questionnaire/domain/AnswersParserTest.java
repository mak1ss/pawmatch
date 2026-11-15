package com.pawmatch.questionnaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.pawmatch.common.kernel.FieldError;
import com.pawmatch.common.kernel.RequestValidationException;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnswersParserTest {

    private static final List<String> SIZES = List.of("XSmall", "Small", "Medium", "Large", "XLarge");

    @Test
    void parsesExampleProfile() {
        Answers answers = TestAnswers.valid();

        assertThat(answers.activityLevel()).isEqualTo(3);
        assertThat(answers.preferredSize()).isEqualTo(PreferredSize.SMALL);
        assertThat(answers.hasYoungChildren()).isTrue();
        assertThat(answers.housingType()).isEqualTo(HousingType.APARTMENT);
        assertThat(answers.walkMinutesDaily()).isEqualTo(60.0);
        assertThat(answers.ownerExperience()).isEqualTo(OwnerExperience.NONE);
        assertThat(answers.allowedSizes()).isEmpty();
        assertThat(answers.hardLimits()).isEmpty();
    }

    @Test
    void collectsEveryErrorSortedByField() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.remove("has_cats");
        raw.put("activity_level", 9);
        raw.put("housing_type", "castle");
        raw.put("favourite_colour", "blue");

        assertThat(fieldErrors(raw)).extracting(FieldError::field, FieldError::code).containsExactly(
                Tuple.tuple("activity_level", "invalid_integer"),
                Tuple.tuple("favourite_colour", "unknown_field"),
                Tuple.tuple("has_cats", "required"),
                Tuple.tuple("housing_type", "invalid_choice"));
    }

    @Test
    void missingFieldsAreRequired() {
        assertThat(fieldErrors(Map.of()))
                .hasSize(14)
                .allSatisfy(e -> {
                    assertThat(e.code()).isEqualTo("required");
                    assertThat(e.message()).isEqualTo("Обов’язкове поле.");
                    assertThat(e.params()).isNull();
                });
    }

    @Test
    void unknownFieldIsRejected() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("dog_name", "Rex");

        assertThat(fieldErrors(raw)).containsExactly(new FieldError("dog_name", "unknown_field", "Невідоме поле."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"int-as-string", "decimal", "boolean", "zero", "six", "null", "huge"})
    void scaleMustBeIntegerOneToFive(String variant) {
        Object value = switch (variant) {
            case "int-as-string" -> "3";
            case "decimal" -> 3.0;
            case "boolean" -> true;
            case "zero" -> 0;
            case "six" -> 6;
            case "null" -> null;
            default -> new BigInteger("99999999999999999999");
        };
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("activity_level", value);

        assertThat(fieldErrors(raw)).containsExactly(new FieldError("activity_level", "invalid_integer",
                "Потрібне ціле число від 1 до 5.", Map.of("min", 1, "max", 5)));
    }

    @Test
    void booleansAreStrict() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("has_cats", 1);
        raw.put("has_other_dogs", "false");

        assertThat(fieldErrors(raw)).containsExactly(
                new FieldError("has_cats", "invalid_boolean", "Потрібне значення true або false."),
                new FieldError("has_other_dogs", "invalid_boolean", "Потрібне значення true або false."));
    }

    @Test
    void choicesListAllowedValues() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("preferred_size", "small");

        assertThat(fieldErrors(raw)).containsExactly(new FieldError("preferred_size", "invalid_choice",
                "Допустимі значення: XSmall, Small, Medium, Large, XLarge, any.",
                Map.of("allowed", List.of("XSmall", "Small", "Medium", "Large", "XLarge", "any"))));
    }

    @Test
    void numbersAcceptDecimalsWithinRange() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("walk_minutes_daily", 45.5);
        raw.put("alone_hours_daily", 0);
        raw.put("training_minutes_daily", 1440);

        Answers answers = AnswersParser.parse(raw);

        assertThat(answers.walkMinutesDaily()).isEqualTo(45.5);
        assertThat(answers.trainingMinutesDaily()).isEqualTo(1440.0);
    }

    @Test
    void numbersOutOfRangeOrWrongTypeAreRejected() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("walk_minutes_daily", -1);
        raw.put("alone_hours_daily", 24.5);
        raw.put("training_minutes_daily", "20");

        assertThat(fieldErrors(raw)).containsExactly(
                new FieldError("alone_hours_daily", "invalid_number", "Потрібне число від 0 до 24.",
                        Map.of("min", 0, "max", 24)),
                new FieldError("training_minutes_daily", "invalid_number", "Потрібне число від 0 до 1440.",
                        Map.of("min", 0, "max", 1440)),
                new FieldError("walk_minutes_daily", "invalid_number", "Потрібне число від 0 до 1440.",
                        Map.of("min", 0, "max", 1440)));
    }

    @Test
    void allowedSizesAreParsedAndSorted() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("allowed_sizes", List.of("Medium", "XSmall"));

        assertThat(AnswersParser.parse(raw).allowedSizes()).containsExactly(DogSize.XSMALL, DogSize.MEDIUM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "unknown", "not-a-list", "any"})
    void invalidAllowedSizesAreRejected(String variant) {
        Object value = switch (variant) {
            case "empty" -> List.of();
            case "unknown" -> List.of("Small", "Huge");
            case "any" -> List.of("any");
            default -> "Small";
        };
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("allowed_sizes", value);

        assertThat(fieldErrors(raw)).singleElement().satisfies(e -> {
            assertThat(e.field()).isEqualTo("allowed_sizes");
            assertThat(e.code()).isEqualTo("invalid_list");
            assertThat(e.params()).containsEntry("allowed", SIZES);
        });
    }

    @Test
    void duplicateAllowedSizesAreRejected() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("allowed_sizes", List.of("Small", "Small"));

        assertThat(fieldErrors(raw)).containsExactly(
                new FieldError("allowed_sizes", "duplicate_value", "Розміри не повинні повторюватися."));
    }

    @Test
    void hardLimitsAreParsed() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("hard_limits", Map.of("energy_level", 3, "coat_grooming_frequency", 2));

        assertThat(AnswersParser.parse(raw).hardLimits()).containsExactlyInAnyOrderEntriesOf(Map.of(
                HardLimitCriterion.ENERGY_LEVEL, 3, HardLimitCriterion.COAT_GROOMING_FREQUENCY, 2));
    }

    @Test
    void invalidHardLimitsReportNestedFields() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("hard_limits", Map.of("energy_level", 7, "size", 2));

        assertThat(fieldErrors(raw)).containsExactlyInAnyOrder(
                new FieldError("hard_limits.energy_level", "invalid_integer", "Потрібне ціле число від 1 до 5.",
                        Map.of("min", 1, "max", 5)),
                new FieldError("hard_limits.size", "unsupported_key", "Непідтримуваний критерій.",
                        Map.of("allowed", Coded.codes(HardLimitCriterion.class))));
    }

    @Test
    void hardLimitsMustBeAnObject() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("hard_limits", List.of(3));

        assertThat(fieldErrors(raw)).containsExactly(
                new FieldError("hard_limits", "invalid_object", "Потрібен об’єкт з максимальними оцінками."));
    }

    @Test
    void everyErrorCodeIsDistinctAndSnakeCase() {
        assertThat(Coded.codes(AnswersParser.ErrorCode.class))
                .doesNotHaveDuplicates()
                .allSatisfy(code -> assertThat(code).matches("[a-z]+(_[a-z]+)*"));
    }

    @Test
    void toMapRoundTripsThroughParse() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("walk_minutes_daily", 45.5);
        raw.put("allowed_sizes", List.of("Small", "XSmall"));
        raw.put("hard_limits", Map.of("barking_level", 2));
        Answers answers = AnswersParser.parse(raw);

        Map<String, Object> canonical = AnswersParser.toMap(answers);

        assertThat(AnswersParser.parse(canonical)).isEqualTo(answers);
        assertThat(canonical).containsEntry("walk_minutes_daily", 45.5)
                .containsEntry("alone_hours_daily", 4L)
                .containsEntry("preferred_size", "Small")
                .containsEntry("allowed_sizes", List.of("XSmall", "Small"));
    }

    @Test
    void toMapOmitsUnsetOptionalFields() {
        assertThat(AnswersParser.toMap(TestAnswers.valid())).doesNotContainKeys("allowed_sizes", "hard_limits");
    }

    @Test
    void nullBodyIsRejected() {
        assertThatThrownBy(() -> AnswersParser.parse(null)).isInstanceOf(RequestValidationException.class);
    }

    private static List<FieldError> fieldErrors(Map<String, ?> raw) {
        try {
            AnswersParser.parse(raw);
        } catch (RequestValidationException e) {
            return e.getFieldErrors();
        }
        throw new AssertionError("Expected validation to fail");
    }
}
