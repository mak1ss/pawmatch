package com.pawmatch.recommendation.adapter.in.web;

import java.util.Map;

import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.recommendation.application.port.in.CompareBreedsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Compares 2–5 breeds against a session's latest answers. Nothing is stored, so the call can be repeated freely.
 * The request schema and examples are documented in {@code resources/openapi/recommendation.yaml}.
 */
@Tag(name = "Compare", description = "Compare chosen breeds side by side against a session's latest answers")
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/compare")
class CompareController {

    private final CompareBreedsUseCase compareBreeds;
    private final ListBreedsUseCase listBreeds;

    CompareController(CompareBreedsUseCase compareBreeds, ListBreedsUseCase listBreeds) {
        this.compareBreeds = compareBreeds;
        this.listBreeds = listBreeds;
    }

    @Operation(summary = "Compare breeds against the session's latest answers",
            description = "Scores each requested breed in the requested order. Excluded breeds return their reasons "
                    + "and no index. The result is not stored. 422 lists every problem with breed_ids; 404 if the "
                    + "session doesn't exist. The response has no score_meaning: the frontend shows the "
                    + "\"index, not a probability\" disclaimer itself.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    ref = "#/components/requestBodies/CompareBreeds"))
    @ApiResponse(responseCode = "200", description = "Comparison", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ComparisonResponse.class),
            examples = @ExampleObject(name = "example-profile", ref = "#/components/examples/Comparison")))
    @PostMapping
    ComparisonResponse compare(@PathVariable String sessionId, @RequestBody Map<String, Object> request) {
        return ComparisonResponse.from(compareBreeds.compare(SessionIds.parse(sessionId), request),
                BreedImageUrls.resolver(listBreeds));
    }
}
