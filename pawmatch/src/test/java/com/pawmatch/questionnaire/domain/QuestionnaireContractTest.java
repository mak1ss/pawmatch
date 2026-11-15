package com.pawmatch.questionnaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.questionnaire.domain.QuestionnaireContract.Field;
import com.pawmatch.questionnaire.domain.QuestionnaireContract.FieldType;
import org.junit.jupiter.api.Test;

/** The published contract must describe exactly what {@link AnswersParser} accepts. */
class QuestionnaireContractTest {

    private final List<Field> fields = QuestionnaireContract.current().fields();

    @Test
    void definesExactlyTheFieldsTheParserAccepts() {
        Set<String> required = fields.stream().filter(Field::required).map(Field::key).collect(Collectors.toSet());
        Set<String> optional = fields.stream().filter(f -> !f.required()).map(Field::key).collect(Collectors.toSet());

        assertThat(required).isEqualTo(AnswersParser.REQUIRED);
        assertThat(optional).isEqualTo(AnswersParser.OPTIONAL);
        assertThat(fields).extracting(Field::key).doesNotHaveDuplicates();
    }

    @Test
    void optionsMatchTheParserCodes() {
        assertThat(options("preferred_size")).containsExactlyElementsOf(Coded.codes(PreferredSize.class));
        assertThat(options("housing_type")).containsExactlyElementsOf(Coded.codes(HousingType.class));
        assertThat(options("owner_experience")).containsExactlyElementsOf(Coded.codes(OwnerExperience.class));
        assertThat(options("allowed_sizes")).containsExactlyElementsOf(Coded.codes(DogSize.class));
        assertThat(options("hard_limits")).containsExactlyElementsOf(Coded.codes(HardLimitCriterion.class));
    }

    @Test
    void everyFieldHasTheAttributesItsTypeNeeds() {
        assertThat(fields).allSatisfy(f -> {
            switch (f.type()) {
                case INTEGER, NUMBER -> assertThat(List.of(f.min(), f.max())).doesNotContainNull();
                case CHOICE, MULTI_CHOICE -> assertThat(f.options()).isNotEmpty();
                case LIMITS -> {
                    assertThat(f.options()).isNotEmpty();
                    assertThat(List.of(f.min(), f.max())).doesNotContainNull();
                }
                case BOOLEAN -> assertThat(f.options()).isEmpty();
            }
        });
    }

    @Test
    void publishedBoundsAreTheBoundsTheParserEnforces() {
        for (Field field : fields) {
            if (field.type() != FieldType.INTEGER && field.type() != FieldType.NUMBER) {
                continue;
            }
            assertThat(acceptsValue(field.key(), field.min())).as(field.key() + " accepts min").isTrue();
            assertThat(acceptsValue(field.key(), field.max())).as(field.key() + " accepts max").isTrue();
            assertThat(acceptsValue(field.key(), field.min() - 1)).as(field.key() + " rejects min - 1").isFalse();
            assertThat(acceptsValue(field.key(), field.max() + 1)).as(field.key() + " rejects max + 1").isFalse();
        }
    }

    private static boolean acceptsValue(String key, int value) {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put(key, value);
        try {
            AnswersParser.parse(raw);
            return true;
        } catch (RequestValidationException e) {
            return false;
        }
    }

    private List<String> options(String key) {
        return fields.stream().filter(f -> f.key().equals(key)).findFirst().orElseThrow().options();
    }
}
