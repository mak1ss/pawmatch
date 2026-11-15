package com.pawmatch.questionnaire.adapter.in.web;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.application.port.in.GetLatestAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.SubmitAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.UpdateAnswersUseCase;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Questionnaire sessions: submit and resubmit answers.
 * <p>
 * Request bodies are taken as a plain map on purpose: the domain parser must see the raw JSON types to reject
 * {@code 1} for a boolean or {@code "3"} for a number, which typed binding would silently coerce.
 * <p>
 * The answers request body (schema and examples) is documented in {@code resources/openapi/questionnaire.yaml}
 * and referenced here by name.
 */
@Tag(name = "Questionnaire sessions", description = "Submit and resubmit questionnaire answers")
@RestController
@RequestMapping("/api/v1/sessions")
class SessionController {

    private static final String ANSWERS_BODY = "#/components/requestBodies/QuestionnaireAnswers";

    private final SubmitAnswersUseCase submitAnswers;
    private final UpdateAnswersUseCase updateAnswers;
    private final GetLatestAnswersUseCase getLatestAnswers;

    SessionController(SubmitAnswersUseCase submitAnswers, UpdateAnswersUseCase updateAnswers,
                      GetLatestAnswersUseCase getLatestAnswers) {
        this.submitAnswers = submitAnswers;
        this.updateAnswers = updateAnswers;
        this.getLatestAnswers = getLatestAnswers;
    }

    @Operation(summary = "Start a session with answers",
            description = "Validates and stores answers version 1. 422 lists every invalid field.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(ref = ANSWERS_BODY))
    @PostMapping
    ResponseEntity<SubmittedAnswersResponse> start(@RequestBody Map<String, Object> answers) {
        SubmittedAnswers stored = submitAnswers.startSession(answers);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}/answers")
                .buildAndExpand(stored.sessionId())
                .toUri();
        return ResponseEntity.created(location).body(SubmittedAnswersResponse.from(stored));
    }

    @Operation(summary = "Change the answers of a session",
            description = "Validates and stores the next answers version. Send the complete answers, not only the "
                    + "changed ones. 404 if the session doesn't exist.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(ref = ANSWERS_BODY))
    @PutMapping("/{sessionId}/answers")
    SubmittedAnswersResponse update(@PathVariable String sessionId, @RequestBody Map<String, Object> answers) {
        return SubmittedAnswersResponse.from(updateAnswers.updateAnswers(parseId(sessionId), answers));
    }

    @Operation(summary = "Get the latest answers of a session", description = "Used to prefill the questionnaire.")
    @GetMapping("/{sessionId}/answers")
    SessionAnswersResponse latest(@PathVariable String sessionId) {
        return SessionAnswersResponse.from(getLatestAnswers.getLatestAnswers(parseId(sessionId)));
    }

    /** A malformed id can't match any session, so it gets the same 404 as an unknown one. */
    private static UUID parseId(String sessionId) {
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("questionnaire_session", sessionId);
        }
    }
}
