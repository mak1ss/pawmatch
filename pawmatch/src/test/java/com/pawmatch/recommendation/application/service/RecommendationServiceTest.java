package com.pawmatch.recommendation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pawmatch.catalog.domain.TestBreeds;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import com.pawmatch.questionnaire.domain.TestAnswers;
import com.pawmatch.recommendation.application.port.out.RecommendationRunRepository;
import com.pawmatch.recommendation.application.port.out.ScoringEngine;
import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.EngineOutcome;
import com.pawmatch.recommendation.domain.RecommendationPolicy;
import com.pawmatch.recommendation.domain.RecommendationRun;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {

    private static final UUID SESSION = UUID.fromString("0193a1ef-9e10-7c3a-9b00-000000000001");

    private final List<RecommendationRun> stored = new ArrayList<>();
    private final RecordingEngine engine = new RecordingEngine();
    private String recommendedSlug = "labrador-retriever";

    private final RecommendationService service = new RecommendationService(
            sessionId -> {
                if (!sessionId.equals(SESSION)) {
                    throw new ResourceNotFoundException("questionnaire_session", sessionId.toString());
                }
                return new SubmittedAnswers(SESSION, 3, "1.0.0", TestAnswers.valid(), Instant.now());
            },
            () -> List.of(TestBreeds.labrador()),
            () -> "1.0.0",
            engine,
            new RecommendationRunRepository() {
                @Override
                public RecommendationRun save(RecommendationRun run) {
                    RecommendationRun saved = new RecommendationRun(UUID.randomUUID(), run.sessionId(),
                            run.answersVersion(), run.datasetVersion(), run.engineUsed(), run.fallbackUsed(),
                            run.latencyMs(), Instant.now(), run.result(), run.items());
                    stored.add(saved);
                    return saved;
                }

                @Override
                public Optional<RecommendationRun> findLatestBySessionId(UUID sessionId) {
                    return stored.stream().filter(r -> r.sessionId().equals(sessionId)).reduce((a, b) -> b);
                }
            });

    @Test
    void scoresTheLatestAnswersAndStoresTheRun() {
        RecommendationRun run = service.recommend(SESSION);

        assertThat(run.id()).isNotNull();
        assertThat(run.answersVersion()).isEqualTo(3);
        assertThat(run.datasetVersion()).isEqualTo("1.0.0");
        assertThat(run.engineUsed()).isEqualTo("test-engine");
        assertThat(run.fallbackUsed()).isTrue();
        assertThat(run.latencyMs()).isNotNegative();
        assertThat(run.items()).singleElement().satisfies(item -> {
            assertThat(item.rank()).isEqualTo(1);
            assertThat(item.breedId()).isEqualTo("labrador-retriever");
            assertThat(item.breedUuid()).isEqualTo(TestBreeds.LABRADOR_ID);
            assertThat(item.compatibilityIndex()).isEqualByComparingTo("88.5");
        });
        assertThat(service.getLatestRecommendation(SESSION)).isEqualTo(run);
    }

    @Test
    void passesMappedInputsAndThePolicyToTheEngine() {
        service.recommend(SESSION);

        assertThat(engine.profile.activityLevel()).isEqualTo(3);
        assertThat(engine.profile.hasYoungChildren()).isTrue();
        assertThat(engine.breeds).extracting(BreedFeatures::breedId).containsExactly("labrador-retriever");
        assertThat(engine.limit).isEqualTo(RecommendationPolicy.LIMIT);
        assertThat(engine.minScore).isEqualByComparingTo(RecommendationPolicy.MIN_SCORE);
    }

    @Test
    void unknownSessionIsNotFoundAndNothingIsStored() {
        assertThatThrownBy(() -> service.recommend(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(stored).isEmpty();
    }

    @Test
    void sessionWithoutRunsHasNoLatestRecommendation() {
        assertThatThrownBy(() -> service.getLatestRecommendation(SESSION))
                .isInstanceOfSatisfying(ResourceNotFoundException.class,
                        e -> assertThat(e.getResource()).isEqualTo("recommendation_run"));
    }

    @Test
    void breedMissingFromTheCatalogueIsAServerError() {
        recommendedSlug = "unknown-breed";

        assertThatIllegalStateException().isThrownBy(() -> service.recommend(SESSION))
                .withMessageContaining("unknown-breed");
        assertThat(stored).isEmpty();
    }

    private final class RecordingEngine implements ScoringEngine {
        ScoringProfile profile;
        List<BreedFeatures> breeds;
        int limit;
        BigDecimal minScore;

        @Override
        public EngineOutcome<ScoringResult> recommend(ScoringProfile profile, List<BreedFeatures> breeds, int limit,
                                                      BigDecimal minScore) {
            this.profile = profile;
            this.breeds = breeds;
            this.limit = limit;
            this.minScore = minScore;
            ScoringResult.ScoredBreed breed = new ScoringResult.ScoredBreed(recommendedSlug, "Лабрадор",
                    "Labrador", true, List.of(), new BigDecimal("88.5"), List.of(), List.of(), List.of(), "https://x");
            return new EngineOutcome<>(new ScoringResult("m", "t", "s", minScore, 1, 1, 1, List.of(breed), "n",
                    List.of(), List.of()), "test-engine", true);
        }

        @Override
        public EngineOutcome<ComparisonResult> compare(ScoringProfile profile, List<BreedFeatures> breeds) {
            throw new UnsupportedOperationException();
        }
    }
}
