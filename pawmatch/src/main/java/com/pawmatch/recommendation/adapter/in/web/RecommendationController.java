package com.pawmatch.recommendation.adapter.in.web;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.recommendation.application.port.in.GetLatestRecommendationUseCase;
import com.pawmatch.recommendation.application.port.in.RecommendUseCase;
import com.pawmatch.recommendation.domain.RecommendationRun;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Recommendations for a questionnaire session. Flow: submit answers ({@code POST /sessions}), then
 * {@code POST /sessions/{id}/recommendations}; after changing answers, call it again to recompute.
 * The response example is documented in {@code resources/openapi/recommendation.yaml}.
 */
@Tag(name = "Recommendations", description = "Score a session's latest answers and get breed recommendations")
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/recommendations")
class RecommendationController {

    private static final String RUN_EXAMPLE = "#/components/examples/RecommendationRun";

    private final RecommendUseCase recommend;
    private final GetLatestRecommendationUseCase latestRecommendation;
    private final ListBreedsUseCase listBreeds;

    RecommendationController(RecommendUseCase recommend, GetLatestRecommendationUseCase latestRecommendation,
                             ListBreedsUseCase listBreeds) {
        this.recommend = recommend;
        this.latestRecommendation = latestRecommendation;
        this.listBreeds = listBreeds;
    }

    @Operation(summary = "Recommend breeds for the session's latest answers",
            description = "Runs the scoring engine and stores the run. At most 5 breeds with an index of at least 60; "
                    + "fewer are returned (never padded) when not enough breeds qualify. 404 if the session doesn't exist.")
    @ApiResponse(responseCode = "201", description = "Recommendation run created", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = RecommendationRunResponse.class),
            examples = @ExampleObject(name = "example-profile", ref = RUN_EXAMPLE)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RecommendationRunResponse create(@PathVariable String sessionId) {
        return toResponse(recommend.recommend(parseId(sessionId)));
    }

    @Operation(summary = "Get the session's latest recommendation run",
            description = "404 if the session doesn't exist or has no recommendation run yet.")
    @ApiResponse(responseCode = "200", description = "Latest recommendation run", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = RecommendationRunResponse.class),
            examples = @ExampleObject(name = "example-profile", ref = RUN_EXAMPLE)))
    @GetMapping("/latest")
    RecommendationRunResponse latest(@PathVariable String sessionId) {
        return toResponse(latestRecommendation.getLatestRecommendation(parseId(sessionId)));
    }

    private RecommendationRunResponse toResponse(RecommendationRun run) {
        Set<String> withImage = listBreeds.listBreeds().stream()
                .filter(Breed::hasImage).map(Breed::slug).collect(Collectors.toSet());
        Function<String, String> imageUrl = slug -> withImage.contains(slug)
                ? ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/breeds/{slug}/image").buildAndExpand(slug).toUriString()
                : null;
        return RecommendationRunResponse.from(run, imageUrl);
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
