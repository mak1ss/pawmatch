package com.pawmatch.recommendation.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pawmatch.catalog.application.port.in.GetCatalogueVersionUseCase;
import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.questionnaire.application.port.in.GetLatestAnswersUseCase;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import com.pawmatch.recommendation.application.port.in.CompareBreedsUseCase;
import com.pawmatch.recommendation.application.port.out.ScoringEngine;
import com.pawmatch.recommendation.domain.BreedComparison;
import com.pawmatch.recommendation.domain.BreedSelection;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.EngineOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Compares chosen breeds against a session's latest answers. Answers and breeds are read through the questionnaire's
 * and catalogue's inbound ports. The request is validated first (like answer submission), then the session is looked up.
 */
@Slf4j
@Service
class ComparisonService implements CompareBreedsUseCase {

    private final GetLatestAnswersUseCase latestAnswers;
    private final ListBreedsUseCase listBreeds;
    private final GetCatalogueVersionUseCase catalogueVersion;
    private final ScoringEngine scoringEngine;

    ComparisonService(GetLatestAnswersUseCase latestAnswers, ListBreedsUseCase listBreeds,
                      GetCatalogueVersionUseCase catalogueVersion, ScoringEngine scoringEngine) {
        this.latestAnswers = latestAnswers;
        this.listBreeds = listBreeds;
        this.catalogueVersion = catalogueVersion;
        this.scoringEngine = scoringEngine;
    }

    @Override
    public BreedComparison compare(UUID sessionId, Map<String, Object> rawRequest) {
        Map<String, Breed> bySlug = listBreeds.listBreeds().stream()
                .collect(Collectors.toMap(Breed::slug, Function.identity()));
        BreedSelection selection = BreedSelection.parse(rawRequest, bySlug.keySet());
        SubmittedAnswers answers = latestAnswers.getLatestAnswers(sessionId);
        List<Breed> chosen = selection.breedIds().stream().map(bySlug::get).toList();

        long started = System.nanoTime();
        EngineOutcome<ComparisonResult> outcome = scoringEngine.compare(ScoringInputs.profile(answers.answers()),
                chosen.stream().map(ScoringInputs::features).toList());
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        // Ids and timings only: never answers or other user input.
        log.info("Comparison for answers v{}: engine={}, fallback={}, model={}, latencyMs={}, breeds={}",
                answers.version(), outcome.engineUsed(), outcome.fallbackUsed(), outcome.result().modelVersion(),
                latencyMs, chosen.size());
        return new BreedComparison(sessionId, answers.version(), catalogueVersion.getCatalogueVersion(),
                outcome.engineUsed(), outcome.fallbackUsed(), latencyMs, outcome.result());
    }
}
