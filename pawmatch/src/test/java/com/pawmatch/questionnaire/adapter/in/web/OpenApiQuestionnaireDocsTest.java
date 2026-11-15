package com.pawmatch.questionnaire.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pawmatch.questionnaire.domain.AnswersParser;
import com.pawmatch.questionnaire.domain.QuestionnaireContract;
import com.pawmatch.questionnaire.domain.QuestionnaireContract.Field;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the hand-written docs in {@code resources/openapi/questionnaire.yaml} against drifting from the code:
 * the schema must match {@link QuestionnaireContract}, and every example must pass the real validator.
 */
class OpenApiQuestionnaireDocsTest {

    private static Components components;
    private static Schema<?> answers;

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = OpenApiQuestionnaireDocsTest.class.getResourceAsStream("/openapi/questionnaire.yaml")) {
            components = Yaml.mapper().readValue(in, Components.class);
        }
        answers = components.getSchemas().get("QuestionnaireAnswers");
    }

    @Test
    void everyExampleIsAValidAnswersObject() {
        assertThat(components.getExamples()).containsKeys("AnswersRequiredOnly", "AnswersWithConstraints");
        for (Map.Entry<String, Example> example : components.getExamples().entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) example.getValue().getValue();
            assertThatNoException().as(example.getKey()).isThrownBy(() -> AnswersParser.parse(value));
        }
    }

    @Test
    void requestBodyReferencesTheSchemaAndExamples() {
        var json = components.getRequestBodies().get("QuestionnaireAnswers").getContent().get("application/json");

        assertThat(json.getSchema().get$ref()).isEqualTo("#/components/schemas/QuestionnaireAnswers");
        assertThat(json.getExamples().values()).extracting(Example::get$ref).containsExactlyInAnyOrder(
                "#/components/examples/AnswersRequiredOnly", "#/components/examples/AnswersWithConstraints");
    }

    @Test
    void schemaFieldsMatchTheContract() {
        assertThat(new HashSet<>(answers.getRequired())).isEqualTo(AnswersParser.REQUIRED);
        Set<String> all = new HashSet<>(AnswersParser.REQUIRED);
        all.addAll(AnswersParser.OPTIONAL);
        assertThat(answers.getProperties().keySet()).isEqualTo(all);
    }

    @Test
    void schemaTypesRangesAndCodesMatchTheContract() {
        for (Field field : QuestionnaireContract.current().fields()) {
            Schema<?> property = answers.getProperties().get(field.key());
            switch (field.type()) {
                case INTEGER, NUMBER -> {
                    assertThat(property.getType()).as(field.key())
                            .isEqualTo(field.type() == QuestionnaireContract.FieldType.INTEGER ? "integer" : "number");
                    assertBounds(field.key(), property, field);
                }
                case BOOLEAN -> assertThat(property.getType()).as(field.key()).isEqualTo("boolean");
                case CHOICE -> assertThat(enumValues(property)).as(field.key()).isEqualTo(field.options());
                case MULTI_CHOICE -> {
                    assertThat(property.getType()).as(field.key()).isEqualTo("array");
                    assertThat(enumValues(property.getItems())).as(field.key()).isEqualTo(field.options());
                }
                case LIMITS -> {
                    assertThat(List.copyOf(property.getProperties().keySet())).as(field.key()).isEqualTo(field.options());
                    property.getProperties().forEach((key, limit) -> assertBounds(field.key() + "." + key, limit, field));
                }
            }
        }
    }

    private static void assertBounds(String name, Schema<?> property, Field field) {
        assertThat(property.getMinimum()).as(name + " minimum").isEqualByComparingTo(BigDecimal.valueOf(field.min()));
        assertThat(property.getMaximum()).as(name + " maximum").isEqualByComparingTo(BigDecimal.valueOf(field.max()));
    }

    private static List<String> enumValues(Schema<?> schema) {
        return schema.getEnum().stream().map(String::valueOf).toList();
    }
}
