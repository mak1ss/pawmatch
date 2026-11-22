package com.pawmatch.recommendation.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.pawmatch.catalog.application.port.in.GetCatalogueVersionUseCase;
import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.application.port.in.GetLatestAnswersUseCase;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import com.pawmatch.recommendation.application.port.in.GetLatestRecommendationUseCase;
import com.pawmatch.recommendation.application.port.in.RecommendUseCase;
import com.pawmatch.recommendation.application.port.out.RecommendationRunRepository;
import com.pawmatch.recommendation.application.port.out.ScoringEngine;
import com.pawmatch.recommendation.domain.EngineOutcome;
import com.pawmatch.recommendation.domain.RecommendationPolicy;
import com.pawmatch.recommendation.domain.RecommendationRun;
import com.pawmatch.recommendation.domain.ScoringResult;
import com.pawmatch.recommendation.domain.ScoringResult.ScoredBreed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs the scoring engine on a session's latest answers against the current catalogue and stores the result.
 * Answers and breeds are read through the questionnaire's and catalogue's inbound ports.
 */
@Slf4j
@Service
class RecommendationService implements RecommendUseCase, GetLatestRecommendationUseCase {

    private final GetLatestAnswersUseCase latestAnswers;
    private final ListBreedsUseCase listBreeds;
    private final GetCatalogueVersionUseCase catalogueVersion;
    private final ScoringEngine scoringEngine;
    private final RecommendationRunRepository runs;

    RecommendationService(GetLatestAnswersUseCase latestAnswers, ListBreedsUseCase listBreeds,
                          GetCatalogueVersionUseCase catalogueVersion, ScoringEngine scoringEngine,
                          RecommendationRunRepository runs) {
        this.latestAnswers = latestAnswers;
        this.listBreeds = listBreeds;
        this.catalogueVersion = catalogueVersion;
        this.scoringEngine = scoringEngine;
        this.runs = runs;
    }

    @Override
    public RecommendationRun recommend(UUID sessionId) {
        SubmittedAnswers answers = latestAnswers.getLatestAnswers(sessionId);
        List<Breed> breeds = listBreeds.listBreeds();

        long started = System.nanoTime();
        EngineOutcome<ScoringResult> outcome = scoringEngine.recommend(ScoringInputs.profile(answers.answers()),
                breeds.stream().map(ScoringInputs::features).toList(),
                RecommendationPolicy.LIMIT, RecommendationPolicy.MIN_SCORE);
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        RecommendationRun stored = runs.save(new RecommendationRun(null, sessionId, answers.version(),
                catalogueVersion.getCatalogueVersion(), outcome.engineUsed(), outcome.fallbackUsed(), latencyMs, null,
                outcome.result(), rankedItems(outcome.result(), breeds)));
        // Ids and timings only: never answers or other user input.
        log.info("Recommendation run {} for answers v{}: engine={}, fallback={}, model={}, latencyMs={}, results={}",
                stored.id(), stored.answersVersion(), stored.engineUsed(), stored.fallbackUsed(),
                stored.result().modelVersion(), latencyMs, stored.items().size());
        return stored;
    }

    @Override
    public RecommendationRun getLatestRecommendation(UUID sessionId) {
        return runs.findLatestBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("recommendation_run", sessionId.toString()));
    }

    private static List<RecommendationRun.RankedBreed> rankedItems(ScoringResult result, List<Breed> breeds) {
        Map<String, UUID> idsBySlug = breeds.stream().collect(Collectors.toMap(Breed::slug, Breed::id));
        List<RecommendationRun.RankedBreed> items = new ArrayList<>();
        List<ScoredBreed> recommendations = result.recommendations();
        for (int i = 0; i < recommendations.size(); i++) {
            ScoredBreed breed = recommendations.get(i);
            UUID breedUuid = idsBySlug.get(breed.breedId());
            if (breedUuid == null) {
                throw new IllegalStateException("Engine recommended a breed missing from the catalogue: " + breed.breedId());
            }
            items.add(new RecommendationRun.RankedBreed(i + 1, breedUuid, breed.breedId(), breed.compatibilityIndex()));
        }
        return items;
    }
}
