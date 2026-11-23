package com.pawmatch.recommendation.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.common.web.OpenApiConfig;
import com.pawmatch.recommendation.application.port.in.CompareBreedsUseCase;
import com.pawmatch.recommendation.domain.BreedSelection;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import org.junit.jupiter.api.BeforeAll;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * The compare request body and examples in {@code resources/openapi/recommendation.yaml} are referenced by the
 * endpoint, the request example passes validation, and the response example deserializes into the response type.
 */
@WebMvcTest(CompareController.class)
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class})
@Import(OpenApiConfig.class)
class CompareOpenApiDocsTest {

    private static final String OPERATION = "$.paths['/api/v1/sessions/{sessionId}/compare'].post";

    private static Components components;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    CompareBreedsUseCase compareBreeds;
    @MockitoBean
    ListBreedsUseCase listBreeds;

    @BeforeAll
    static void loadComponents() throws IOException {
        try (InputStream in = CompareOpenApiDocsTest.class.getResourceAsStream("/openapi/recommendation.yaml")) {
            components = Yaml.mapper().readValue(in, Components.class);
        }
    }

    @Test
    void endpointReferencesTheDocumentedBodyAndExample() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(OPERATION + ".requestBody['$ref']")
                        .value("#/components/requestBodies/CompareBreeds"))
                .andExpect(jsonPath(OPERATION + ".responses['200'].content['application/json']"
                        + ".examples['example-profile']['$ref']").value("#/components/examples/Comparison"))
                .andExpect(jsonPath("$.components.schemas.CompareRequest.properties.breed_ids.maxItems").value(5));
    }

    @Test
    @SuppressWarnings("unchecked")
    void requestExamplePassesValidation() {
        Map<String, Object> request = (Map<String, Object>) components.getExamples().get("CompareRequest").getValue();

        BreedSelection selection = BreedSelection.parse(request,
                Set.of("cavalier-king-charles-spaniel", "labrador-retriever", "chihuahua"));

        assertThat(selection.breedIds()).hasSize(3);
    }

    @Test
    void responseExampleDeserializesIntoTheResponseType() {
        Object value = components.getExamples().get("Comparison").getValue();

        ComparisonResponse response = jsonMapper.convertValue(value, ComparisonResponse.class);

        assertThat(response.breeds()).extracting(ComparisonResponse.ComparedBreedResponse::breedId)
                .containsExactly("cavalier-king-charles-spaniel", "labrador-retriever", "chihuahua");
        assertThat(response.breeds().get(1).compatibilityIndex()).isEqualByComparingTo(new BigDecimal("70.0"));
        ComparisonResponse.ComparedBreedResponse excluded = response.breeds().get(2);
        assertThat(excluded.eligible()).isFalse();
        assertThat(excluded.compatibilityIndex()).isNull();
        assertThat(excluded.exclusionReasons()).isNotEmpty();
        assertThat(excluded.factors()).extracting(RecommendationRunResponse.FactorResponse::criterion)
                .containsExactlyElementsOf(List.of("activity", "shedding", "grooming", "mental_stimulation",
                        "barking", "size", "young_children"));
    }
}
