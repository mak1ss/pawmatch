package com.pawmatch.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class OpenApiComponentFilesTest {

    @Test
    void loadsComponentFilesFromTheClasspath() {
        Components components = OpenApiComponentFiles.load(new PathMatchingResourcePatternResolver());

        assertThat(components.getSchemas()).containsKey("QuestionnaireAnswers");
        assertThat(components.getRequestBodies()).containsKey("QuestionnaireAnswers");
        assertThat(components.getExamples()).containsKeys("AnswersRequiredOnly", "AnswersWithConstraints");
    }

    @Test
    void mergesIntoTheGeneratedSpec() {
        OpenAPI spec = new OpenAPI();

        OpenApiComponentFiles.mergeInto(spec, new Components().addExamples("A", new Example().value(1)));

        assertThat(spec.getComponents().getExamples()).containsKey("A");
    }

    @Test
    void duplicateComponentNamesAreRejected() {
        OpenAPI spec = new OpenAPI().components(new Components().addExamples("A", new Example().value(1)));

        assertThatIllegalStateException()
                .isThrownBy(() -> OpenApiComponentFiles.mergeInto(spec, new Components().addExamples("A", new Example())))
                .withMessageContaining("Duplicate OpenAPI example 'A'");
    }
}
