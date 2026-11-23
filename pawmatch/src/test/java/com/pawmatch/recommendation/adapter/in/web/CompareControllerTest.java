package com.pawmatch.recommendation.adapter.in.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.TestBreeds;
import com.pawmatch.common.kernel.FieldError;
import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.recommendation.application.port.in.CompareBreedsUseCase;
import com.pawmatch.recommendation.domain.BreedComparison;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.ScoringResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompareController.class)
class CompareControllerTest {

    private static final UUID SESSION = UUID.fromString("0193a1ef-9e10-7c3a-9b00-000000000001");
    private static final String BODY = """
            {"breed_ids": ["labrador-retriever", "chihuahua"]}""";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CompareBreedsUseCase compareBreeds;
    @MockitoBean
    ListBreedsUseCase listBreeds;

    @Test
    void returns200WithBreedsInSnakeCaseAndExcludedBreedsWithoutAnIndex() throws Exception {
        given(compareBreeds.compare(SESSION, Map.of("breed_ids", List.of("labrador-retriever", "chihuahua"))))
                .willReturn(comparison());
        given(listBreeds.listBreeds()).willReturn(List.of(TestBreeds.labradorWithImage()));

        mockMvc.perform(post("/api/v1/sessions/{id}/compare", SESSION)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value(SESSION.toString()))
                .andExpect(jsonPath("$.answers_version").value(2))
                .andExpect(jsonPath("$.model_version").value("pawmatch-baseline-1.0.0"))
                .andExpect(jsonPath("$.dataset_version").value("1.0.0"))
                .andExpect(jsonPath("$.score_meaning").doesNotExist())
                .andExpect(jsonPath("$.engine_used").doesNotExist())
                .andExpect(jsonPath("$.breeds[0].breed_id").value("labrador-retriever"))
                .andExpect(jsonPath("$.breeds[0].eligible").value(true))
                .andExpect(jsonPath("$.breeds[0].compatibility_index").value(70.0))
                .andExpect(jsonPath("$.breeds[0].factors[0].breed_value").value(5))
                .andExpect(jsonPath("$.breeds[0].image_url")
                        .value("http://localhost/api/v1/breeds/labrador-retriever/image"))
                .andExpect(jsonPath("$.breeds[1].breed_id").value("chihuahua"))
                .andExpect(jsonPath("$.breeds[1].eligible").value(false))
                .andExpect(jsonPath("$.breeds[1].exclusion_reasons[0]").value("Діти."))
                .andExpect(jsonPath("$.breeds[1].compatibility_index").value(nullValue()))
                .andExpect(jsonPath("$.breeds[1].image_url").value(nullValue()));
    }

    @Test
    void invalidRequestIs422WithFieldErrors() throws Exception {
        given(compareBreeds.compare(eq(SESSION), any())).willThrow(new RequestValidationException(List.of(
                new FieldError("breed_ids", "invalid_count", "Для порівняння потрібно 2–5 порід.",
                        Map.of("min", 2, "max", 5)),
                new FieldError("breed_ids", "unknown_breed", "Невідомі породи.", Map.of("unknown", List.of("x"))))));

        mockMvc.perform(post("/api/v1/sessions/{id}/compare", SESSION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"breed_ids\": [\"x\"]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.field_errors[0].field").value("breed_ids"))
                .andExpect(jsonPath("$.field_errors[0].code").value("invalid_count"))
                .andExpect(jsonPath("$.field_errors[0].params.min").value(2))
                .andExpect(jsonPath("$.field_errors[1].params.unknown[0]").value("x"));
    }

    @Test
    void unknownSessionIs404() throws Exception {
        given(compareBreeds.compare(eq(SESSION), any()))
                .willThrow(new ResourceNotFoundException("questionnaire_session", SESSION.toString()));

        mockMvc.perform(post("/api/v1/sessions/{id}/compare", SESSION)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resource").value("questionnaire_session"));
    }

    @Test
    void malformedSessionIdIs404WithoutCallingTheUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/compare", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
        verifyNoInteractions(compareBreeds);
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/compare", SESSION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"breed_ids\": ["))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(compareBreeds);
    }

    private static BreedComparison comparison() {
        ScoringResult.Factor activity = new ScoringResult.Factor("activity", "Активність", 5, 3,
                new BigDecimal("0.5"), 3.0, "Активність породи — 5/5, бажаний рівень — 3/5.");
        ScoringResult.ScoredBreed labrador = new ScoringResult.ScoredBreed("labrador-retriever", "Лабрадор-ретривер",
                "Labrador Retriever", true, List.of(), new BigDecimal("70.0"), List.of(activity), List.of(),
                List.of(), "https://www.akc.org/dog-breeds/labrador-retriever/");
        ScoringResult.ScoredBreed chihuahua = new ScoringResult.ScoredBreed("chihuahua", "Чихуахуа", "Chihuahua",
                false, List.of("Діти."), null, List.of(activity), List.of(), List.of(),
                "https://www.akc.org/dog-breeds/chihuahua/");
        return new BreedComparison(SESSION, 2, "1.0.0", "baseline", false, 1,
                new ComparisonResult("pawmatch-baseline-1.0.0", List.of(labrador, chihuahua)));
    }
}
