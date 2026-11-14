package com.pawmatch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
}
