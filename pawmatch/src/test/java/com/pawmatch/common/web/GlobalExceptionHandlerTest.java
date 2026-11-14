package com.pawmatch.common.web;

import com.pawmatch.common.kernel.FieldError;
import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.common.kernel.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class,
        properties = "pawmatch.cors.allowed-origins=http://localhost:5173")
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void beanValidationErrorsReturn422WithSnakeCaseFieldErrors() throws Exception {
        mockMvc.perform(post("/api/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity_level\": 9}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.field_errors", hasSize(2)))
                .andExpect(jsonPath("$.field_errors[*].field",
                        containsInAnyOrder("activity_level", "has_cats")));
    }

    @Test
    void snakeCaseJsonIsBoundToCamelCaseFields() throws Exception {
        mockMvc.perform(post("/api/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity_level\": 3, \"has_cats\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity_level").value(3))
                .andExpect(jsonPath("$.has_cats").value(false));
    }

    @Test
    void domainValidationErrorsReturn422() throws Exception {
        mockMvc.perform(get("/api/test/domain-validation"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.field_errors[0].field").value("preferred_size"))
                .andExpect(jsonPath("$.field_errors[0].message").value("Недопустиме значення."));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingResourceReturns404() throws Exception {
        mockMvc.perform(get("/api/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resource").value("breed"))
                .andExpect(jsonPath("$.id").value("unknown-breed"));
    }

    @Test
    void unexpectedErrorsReturn500WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/api/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Внутрішня помилка сервера. Спробуйте пізніше."));
    }

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/test/validated")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/test/validated")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void toSnakeCaseConvertsCamelCase() {
        assertThat(GlobalExceptionHandler.toSnakeCase("activityLevel")).isEqualTo("activity_level");
        assertThat(GlobalExceptionHandler.toSnakeCase("hardLimits.energyLevel")).isEqualTo("hard_limits.energy_level");
        assertThat(GlobalExceptionHandler.toSnakeCase("limit")).isEqualTo("limit");
    }

    record TestRequest(
            @NotNull(message = "Обов’язкове поле.") @Min(1) @Max(value = 5, message = "Потрібне ціле число 1–5.")
            Integer activityLevel,
            @NotNull(message = "Обов’язкове поле.") Boolean hasCats) {
    }

    @RestController
    static class TestController {

        @PostMapping("/api/test/validated")
        TestRequest validated(@Valid @RequestBody TestRequest request) {
            return request;
        }

        @GetMapping("/api/test/domain-validation")
        void domainValidation() {
            throw new RequestValidationException(List.of(new FieldError("preferred_size", "Недопустиме значення.")));
        }

        @GetMapping("/api/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("breed", "unknown-breed");
        }

        @GetMapping("/api/test/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
