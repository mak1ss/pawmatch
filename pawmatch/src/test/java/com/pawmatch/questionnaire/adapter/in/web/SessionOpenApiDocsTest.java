package com.pawmatch.questionnaire.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pawmatch.common.web.OpenApiConfig;
import com.pawmatch.questionnaire.application.port.in.GetLatestAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.SubmitAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.UpdateAnswersUseCase;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The generated OpenAPI spec references the hand-written answers request body for both write endpoints, and the
 * referenced components (schema, examples) are merged in. Runs springdoc inside a web slice, no database needed.
 */
@WebMvcTest(SessionController.class)
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class})
@Import(OpenApiConfig.class)
class SessionOpenApiDocsTest {

    private static final String ANSWERS_BODY = "#/components/requestBodies/QuestionnaireAnswers";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubmitAnswersUseCase submitAnswers;
    @MockitoBean
    UpdateAnswersUseCase updateAnswers;
    @MockitoBean
    GetLatestAnswersUseCase getLatestAnswers;

    @Test
    void writeEndpointsReferenceTheDocumentedAnswersBody() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.requestBody['$ref']").value(ANSWERS_BODY))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/answers'].put.requestBody['$ref']")
                        .value(ANSWERS_BODY))
                .andExpect(jsonPath("$.components.requestBodies.QuestionnaireAnswers.content['application/json']"
                        + ".schema['$ref']").value("#/components/schemas/QuestionnaireAnswers"))
                .andExpect(jsonPath("$.components.schemas.QuestionnaireAnswers.required.length()").value(14))
                .andExpect(jsonPath("$.components.examples.AnswersRequiredOnly.value.preferred_size").value("Small"))
                .andExpect(jsonPath("$.components.examples.AnswersWithConstraints.value.hard_limits.barking_level")
                        .value(3));
    }
}
