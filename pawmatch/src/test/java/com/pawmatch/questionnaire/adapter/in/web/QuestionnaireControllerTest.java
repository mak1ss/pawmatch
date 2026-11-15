package com.pawmatch.questionnaire.adapter.in.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pawmatch.questionnaire.application.port.in.GetQuestionnaireUseCase;
import com.pawmatch.questionnaire.domain.QuestionnaireContract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuestionnaireController.class)
class QuestionnaireControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetQuestionnaireUseCase getQuestionnaire;

    @Test
    void returnsTheContractWithoutPresentationCopy() throws Exception {
        given(getQuestionnaire.getQuestionnaire()).willReturn(QuestionnaireContract.current());

        mockMvc.perform(get("/api/v1/questionnaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.fields", hasSize(16)))
                .andExpect(jsonPath("$.fields[0].key").value("activity_level"))
                .andExpect(jsonPath("$.fields[0].type").value("integer"))
                .andExpect(jsonPath("$.fields[0].min").value(1))
                .andExpect(jsonPath("$.fields[0].max").value(5))
                .andExpect(jsonPath("$.fields[0].options").doesNotExist())
                .andExpect(jsonPath("$.fields[?(@.key == 'preferred_size')].options[*]", hasItem("any")))
                .andExpect(jsonPath("$.fields[?(@.key == 'has_cats')].type", hasItem("boolean")))
                .andExpect(jsonPath("$.fields[?(@.key == 'walk_minutes_daily')].max", hasItem(1440)))
                .andExpect(jsonPath("$.fields[?(@.key == 'hard_limits')].required", hasItem(false)))
                .andExpect(jsonPath("$.sections").doesNotExist())
                .andExpect(jsonPath("$.fields[0].label").doesNotExist());
    }
}
