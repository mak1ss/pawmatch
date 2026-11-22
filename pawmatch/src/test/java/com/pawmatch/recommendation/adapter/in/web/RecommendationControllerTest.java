package com.pawmatch.recommendation.adapter.in.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.TestBreeds;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.recommendation.application.port.in.GetLatestRecommendationUseCase;
import com.pawmatch.recommendation.application.port.in.RecommendUseCase;
import com.pawmatch.recommendation.domain.RecommendationRun;
import com.pawmatch.recommendation.domain.ScoringResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    private static final UUID SESSION = UUID.fromString("0193a1ef-9e10-7c3a-9b00-000000000001");
    private static final UUID RUN = UUID.fromString("0193a1f0-5b2c-7d4e-8f00-000000000001");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RecommendUseCase recommend;
    @MockitoBean
    GetLatestRecommendationUseCase latestRecommendation;
    @MockitoBean
    ListBreedsUseCase listBreeds;

    @Test
    void createReturns201WithTheRunInSnakeCase() throws Exception {
        given(recommend.recommend(SESSION)).willReturn(run());
        given(listBreeds.listBreeds()).willReturn(List.of(TestBreeds.labrador()));

        mockMvc.perform(post("/api/v1/sessions/{id}/recommendations", SESSION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.run_id").value(RUN.toString()))
                .andExpect(jsonPath("$.answers_version").value(2))
                .andExpect(jsonPath("$.model_version").value("pawmatch-baseline-1.0.0"))
                .andExpect(jsonPath("$.score_meaning").value("Індекс, не ймовірність."))
                .andExpect(jsonPath("$.dataset_version").value("1.0.0"))
                .andExpect(jsonPath("$.recommendations[0].breed_id").value("labrador-retriever"))
                .andExpect(jsonPath("$.recommendations[0].compatibility_index").value(88.5))
                .andExpect(jsonPath("$.recommendations[0].factors[0].breed_value").value("Large"))
                .andExpect(jsonPath("$.recommendations[0].factors[1].profile_value").value(true))
                .andExpect(jsonPath("$.recommendations[0].image_url").value(nullValue()))
                .andExpect(jsonPath("$.recommendations[0].eligible").doesNotExist())
                .andExpect(jsonPath("$.excluded[0].breed_id").value("chihuahua"))
                .andExpect(jsonPath("$.engine_used").doesNotExist());
    }

    @Test
    void recommendedBreedWithAPhotoGetsAnAbsoluteImageUrl() throws Exception {
        given(latestRecommendation.getLatestRecommendation(SESSION)).willReturn(run());
        given(listBreeds.listBreeds()).willReturn(List.of(TestBreeds.labradorWithImage()));

        mockMvc.perform(get("/api/v1/sessions/{id}/recommendations/latest", SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].image_url")
                        .value("http://localhost/api/v1/breeds/labrador-retriever/image"));
    }

    @Test
    void unknownSessionOrMissingRunIs404() throws Exception {
        given(recommend.recommend(SESSION))
                .willThrow(new ResourceNotFoundException("questionnaire_session", SESSION.toString()));
        given(latestRecommendation.getLatestRecommendation(SESSION))
                .willThrow(new ResourceNotFoundException("recommendation_run", SESSION.toString()));

        mockMvc.perform(post("/api/v1/sessions/{id}/recommendations", SESSION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resource").value("questionnaire_session"));
        mockMvc.perform(get("/api/v1/sessions/{id}/recommendations/latest", SESSION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resource").value("recommendation_run"));
    }

    @Test
    void malformedSessionIdIs404() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/recommendations", "not-a-uuid"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(recommend);
    }

    private static RecommendationRun run() {
        ScoringResult.ScoredBreed labrador = new ScoringResult.ScoredBreed("labrador-retriever", "Лабрадор-ретривер",
                "Labrador Retriever", true, List.of(), new BigDecimal("88.5"),
                List.of(new ScoringResult.Factor("size", "Розмір", "Large", "Medium", new BigDecimal("0.7500"), 1.0,
                                "Розмір за AKC — Large; бажаний — Medium."),
                        new ScoringResult.Factor("young_children", "Взаємодія з малими дітьми", 5, true,
                                new BigDecimal("1.0000"), 1.5, "Взаємодія з малими дітьми: породна оцінка 5/5.")),
                List.of("Взаємодія з малими дітьми: породна оцінка 5/5."), List.of("Розмір за AKC — Large."),
                "https://www.akc.org/dog-breeds/labrador-retriever/");
        ScoringResult result = new ScoringResult("pawmatch-baseline-1.0.0", "weighted_rule_based_baseline",
                "Індекс, не ймовірність.", BigDecimal.valueOf(60), 30, 29, 1, List.of(labrador),
                "Знайдено лише 1 порід вище порога.", List.of("Тип житла: не оцінюється."),
                List.of(new ScoringResult.Exclusion("chihuahua", List.of("Діти."))));
        return new RecommendationRun(RUN, SESSION, 2, "1.0.0", "baseline", false, 3,
                Instant.parse("2026-11-22T10:00:00Z"), result,
                List.of(new RecommendationRun.RankedBreed(1, TestBreeds.LABRADOR_ID, "labrador-retriever",
                        new BigDecimal("88.5"))));
    }
}
