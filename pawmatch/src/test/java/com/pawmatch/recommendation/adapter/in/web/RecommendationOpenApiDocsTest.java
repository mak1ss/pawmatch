package com.pawmatch.recommendation.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;

import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.common.web.OpenApiConfig;
import com.pawmatch.recommendation.application.port.in.GetLatestRecommendationUseCase;
import com.pawmatch.recommendation.application.port.in.RecommendUseCase;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
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
 * The response example in {@code resources/openapi/recommendation.yaml} is referenced by both endpoints and still
 * deserializes into the real response type.
 */
@WebMvcTest(RecommendationController.class)
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class})
@Import(OpenApiConfig.class)
class RecommendationOpenApiDocsTest {

    private static final String PATH = "$.paths['/api/v1/sessions/{sessionId}/recommendations";
    private static final String EXAMPLE = "#/components/examples/RecommendationRun";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    RecommendUseCase recommend;
    @MockitoBean
    GetLatestRecommendationUseCase latestRecommendation;
    @MockitoBean
    ListBreedsUseCase listBreeds;

    @Test
    void bothEndpointsReferenceTheDocumentedExample() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PATH + "'].post.responses['201'].content['application/json']"
                        + ".examples['example-profile']['$ref']").value(EXAMPLE))
                .andExpect(jsonPath(PATH + "/latest'].get.responses['200'].content['application/json']"
                        + ".examples['example-profile']['$ref']").value(EXAMPLE))
                .andExpect(jsonPath("$.components.examples.RecommendationRun.value.recommendations[0].breed_id")
                        .value("cavalier-king-charles-spaniel"));
    }

    @Test
    void exampleDeserializesIntoTheResponseType() throws IOException {
        Components components;
        try (InputStream in = getClass().getResourceAsStream("/openapi/recommendation.yaml")) {
            components = Yaml.mapper().readValue(in, Components.class);
        }
        Object value = components.getExamples().get("RecommendationRun").getValue();

        RecommendationRunResponse response = jsonMapper.convertValue(value, RecommendationRunResponse.class);

        assertThat(response.recommendations()).hasSize(5);
        assertThat(response.recommendations().getFirst().compatibilityIndex()).isEqualByComparingTo("98.0");
        assertThat(response.scoreMeaning()).isNotBlank();
        assertThat(response.excluded()).extracting(RecommendationRunResponse.ExclusionResponse::breedId)
                .containsExactly("chihuahua");
    }
}
