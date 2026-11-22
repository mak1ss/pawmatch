package com.pawmatch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("docker")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PawmatchApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoadsAndHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiDocsAreServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PawMatch API"));
    }

    @Test
    void breedCatalogueIsServedEndToEnd() throws Exception {
        mockMvc.perform(get("/api/v1/breeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(30));
        mockMvc.perform(get("/api/v1/breeds/labrador-retriever"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name_uk").value("Лабрадор-ретривер"));
        mockMvc.perform(get("/api/v1/breeds/labrador-retriever/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void questionnaireSubmitAndResubmitEndToEnd() throws Exception {
        String answers = """
                {"activity_level": 3, "shedding_tolerance": 2, "grooming_capacity": 2, "mental_stimulation_capacity": 3,
                 "barking_tolerance": 2, "preferred_size": "Small", "has_young_children": true, "has_other_dogs": false,
                 "has_cats": false, "housing_type": "apartment", "walk_minutes_daily": 60, "alone_hours_daily": 4,
                 "owner_experience": "none", "training_minutes_daily": 20}""";

        String location = mockMvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content(answers))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answers_version").value(1))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content(answers.replace("\"activity_level\": 3", "\"activity_level\": 5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers_version").value(2));
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers.activity_level").value(5));
        mockMvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("{\"has_cats\": 1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.field_errors.length()").value(14));
    }

    @Test
    void recommendationsEndToEnd() throws Exception {
        String answers = """
                {"activity_level": 3, "shedding_tolerance": 2, "grooming_capacity": 2, "mental_stimulation_capacity": 3,
                 "barking_tolerance": 2, "preferred_size": "Small", "has_young_children": true, "has_other_dogs": false,
                 "has_cats": false, "housing_type": "apartment", "walk_minutes_daily": 60, "alone_hours_daily": 4,
                 "owner_experience": "none", "training_minutes_daily": 20}""";
        String sessionId = JsonPath.read(mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON).content(answers))
                .andReturn().getResponse().getContentAsString(), "$.session_id");

        mockMvc.perform(get("/api/v1/sessions/{id}/recommendations/latest", sessionId))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/sessions/{id}/recommendations", sessionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answers_version").value(1))
                .andExpect(jsonPath("$.dataset_version").value("1.0.0"))
                .andExpect(jsonPath("$.recommendations.length()").value(5))
                .andExpect(jsonPath("$.recommendations[0].breed_id").value("cavalier-king-charles-spaniel"))
                .andExpect(jsonPath("$.recommendations[0].compatibility_index").value(98.0));
        mockMvc.perform(get("/api/v1/sessions/{id}/recommendations/latest", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[4].breed_id").value("bulldog"));
    }
}
