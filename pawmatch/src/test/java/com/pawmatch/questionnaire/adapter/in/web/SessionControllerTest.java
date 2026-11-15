package com.pawmatch.questionnaire.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pawmatch.common.kernel.FieldError;
import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.application.port.in.GetLatestAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.SubmitAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.UpdateAnswersUseCase;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import com.pawmatch.questionnaire.domain.TestAnswers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    private static final UUID SESSION_ID = UUID.fromString("0190f2a0-0000-7000-8000-000000000001");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-11-15T10:00:00Z");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubmitAnswersUseCase submitAnswers;
    @MockitoBean
    UpdateAnswersUseCase updateAnswers;
    @MockitoBean
    GetLatestAnswersUseCase getLatestAnswers;

    @Test
    void startReturns201WithLocationAndVersion() throws Exception {
        given(submitAnswers.startSession(any())).willReturn(stored(1));

        mockMvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("{\"activity_level\": 3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/sessions/" + SESSION_ID + "/answers"))
                .andExpect(jsonPath("$.session_id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.answers_version").value(1))
                .andExpect(jsonPath("$.questionnaire_version").value("1.0.0"))
                .andExpect(jsonPath("$.submitted_at").value("2026-11-15T10:00:00Z"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rawJsonTypesReachTheUseCaseUncoerced() throws Exception {
        given(submitAnswers.startSession(any())).willReturn(stored(1));

        mockMvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"has_cats": 1, "activity_level": "3", "walk_minutes_daily": 30.5,
                 "allowed_sizes": ["Small"], "hard_limits": {"energy_level": 3}}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(submitAnswers).startSession(captor.capture());
        assertThat(captor.getValue().get("has_cats")).isInstanceOf(Integer.class);
        assertThat(captor.getValue().get("activity_level")).isInstanceOf(String.class);
        assertThat(captor.getValue().get("walk_minutes_daily")).isInstanceOf(Double.class);
        assertThat(captor.getValue().get("allowed_sizes")).isInstanceOf(List.class);
        assertThat(captor.getValue().get("hard_limits")).isInstanceOf(Map.class);
    }

    @Test
    void invalidAnswersReturn422WithFieldErrors() throws Exception {
        given(submitAnswers.startSession(any())).willThrow(new RequestValidationException(List.of(
                new FieldError("has_cats", "invalid_boolean", "Потрібне значення true або false."),
                new FieldError("activity_level", "required", "Обов’язкове поле."))));

        mockMvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("{\"has_cats\": 1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.field_errors[0].field").value("has_cats"))
                .andExpect(jsonPath("$.field_errors[0].code").value("invalid_boolean"))
                .andExpect(jsonPath("$.field_errors[1].message").value("Обов’язкове поле."))
                .andExpect(jsonPath("$.field_errors[1].params").doesNotExist());
    }

    @Test
    void nonObjectBodyIs400() throws Exception {
        mockMvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("[1, 2]"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(submitAnswers);
    }

    @Test
    void updateReturnsNextVersion() throws Exception {
        given(updateAnswers.updateAnswers(eq(SESSION_ID), any())).willReturn(stored(2));

        mockMvc.perform(put("/api/v1/sessions/{id}/answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activity_level\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers_version").value(2));
    }

    @Test
    void unknownSessionIs404() throws Exception {
        given(updateAnswers.updateAnswers(eq(SESSION_ID), any()))
                .willThrow(new ResourceNotFoundException("questionnaire_session", SESSION_ID.toString()));

        mockMvc.perform(put("/api/v1/sessions/{id}/answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resource").value("questionnaire_session"));
    }

    @Test
    void malformedSessionIdIs404() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}/answers", "not-a-uuid"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(getLatestAnswers);
    }

    @Test
    void latestAnswersAreReturnedInSubmittedShape() throws Exception {
        given(getLatestAnswers.getLatestAnswers(SESSION_ID)).willReturn(stored(3));

        mockMvc.perform(get("/api/v1/sessions/{id}/answers", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers_version").value(3))
                .andExpect(jsonPath("$.answers.activity_level").value(3))
                .andExpect(jsonPath("$.answers.preferred_size").value("Small"))
                .andExpect(jsonPath("$.answers.has_young_children").value(true))
                .andExpect(jsonPath("$.answers.walk_minutes_daily").value(60));
    }

    private static SubmittedAnswers stored(int version) {
        return new SubmittedAnswers(SESSION_ID, version, "1.0.0", TestAnswers.valid(), SUBMITTED_AT);
    }
}
