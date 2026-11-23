package com.pawmatch.recommendation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pawmatch.catalog.domain.TestBreeds;
import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import com.pawmatch.questionnaire.domain.TestAnswers;
import com.pawmatch.recommendation.application.port.out.ScoringEngine;
import com.pawmatch.recommendation.domain.BreedComparison;
import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.EngineOutcome;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;
import org.junit.jupiter.api.Test;

class ComparisonServiceTest {

    private static final UUID SESSION = UUID.fromString("0193a1ef-9e10-7c3a-9b00-000000000001");
    private static final UUID BEAGLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PUG_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private final RecordingEngine engine = new RecordingEngine();

    private final ComparisonService service = new ComparisonService(
            sessionId -> {
                if (!sessionId.equals(SESSION)) {
                    throw new ResourceNotFoundException("questionnaire_session", sessionId.toString());
                }
                return new SubmittedAnswers(SESSION, 3, "1.0.0", TestAnswers.valid(), Instant.now());
            },
            () -> List.of(TestBreeds.labrador(), TestBreeds.breed(BEAGLE_ID, "beagle", null),
                    TestBreeds.breed(PUG_ID, "pug", null)),
            () -> "1.0.0",
            engine);

    @Test
    void scoresTheChosenBreedsInTheRequestedOrderAgainstTheLatestAnswers() {
        BreedComparison comparison = service.compare(SESSION,
                Map.of("breed_ids", List.of("pug", "labrador-retriever", "beagle")));

        assertThat(engine.breeds).extracting(BreedFeatures::breedId)
                .containsExactly("pug", "labrador-retriever", "beagle");
        assertThat(engine.profile.activityLevel()).isEqualTo(3);
        assertThat(comparison.sessionId()).isEqualTo(SESSION);
        assertThat(comparison.answersVersion()).isEqualTo(3);
        assertThat(comparison.datasetVersion()).isEqualTo("1.0.0");
        assertThat(comparison.engineUsed()).isEqualTo("test-engine");
        assertThat(comparison.fallbackUsed()).isFalse();
        assertThat(comparison.latencyMs()).isNotNegative();
        assertThat(comparison.result().breeds()).extracting(ScoringResult.ScoredBreed::breedId)
                .containsExactly("pug", "labrador-retriever", "beagle");
    }

    @Test
    void invalidRequestIsRejectedBeforeTheSessionOrEngineIsUsed() {
        assertThatThrownBy(() -> service.compare(UUID.randomUUID(),
                Map.of("breed_ids", List.of("pug", "pug", "unicorn"))))
                .isInstanceOfSatisfying(RequestValidationException.class, e ->
                        assertThat(e.getFieldErrors()).extracting(error -> error.code())
                                .containsExactly("duplicate_value", "unknown_breed"));
        assertThat(engine.breeds).isNull();
    }

    @Test
    void unknownSessionIsNotFound() {
        assertThatThrownBy(() -> service.compare(UUID.randomUUID(), Map.of("breed_ids", List.of("pug", "beagle"))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(engine.breeds).isNull();
    }

    private static final class RecordingEngine implements ScoringEngine {
        ScoringProfile profile;
        List<BreedFeatures> breeds;

        @Override
        public EngineOutcome<ScoringResult> recommend(ScoringProfile profile, List<BreedFeatures> breeds, int limit,
                                                      BigDecimal minScore) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EngineOutcome<ComparisonResult> compare(ScoringProfile profile, List<BreedFeatures> breeds) {
            this.profile = profile;
            this.breeds = breeds;
            List<ScoringResult.ScoredBreed> scored = breeds.stream()
                    .map(b -> new ScoringResult.ScoredBreed(b.breedId(), b.breedName(), b.breedNameEn(), true,
                            List.of(), new BigDecimal("75.0"), List.of(), List.of(), List.of(), b.sourceUrl()))
                    .toList();
            return new EngineOutcome<>(new ComparisonResult("m", scored), "test-engine", false);
        }
    }
}
