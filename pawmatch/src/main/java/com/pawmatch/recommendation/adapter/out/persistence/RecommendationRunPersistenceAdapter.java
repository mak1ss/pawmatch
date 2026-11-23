package com.pawmatch.recommendation.adapter.out.persistence;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pawmatch.recommendation.application.port.out.RecommendationRunRepository;
import com.pawmatch.recommendation.domain.RecommendationRun;
import com.pawmatch.recommendation.domain.ScoringResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link RecommendationRunRepository} backed by {@code recommendation_run} (result as jsonb) and
 * {@code recommendation_item} (one row per recommended breed).
 */
@Component
class RecommendationRunPersistenceAdapter implements RecommendationRunRepository {

    private final RecommendationRunJpaRepository runs;
    private final RecommendationItemJpaRepository items;
    private final JsonMapper jsonMapper;

    RecommendationRunPersistenceAdapter(RecommendationRunJpaRepository runs, RecommendationItemJpaRepository items,
                                        JsonMapper jsonMapper) {
        this.runs = runs;
        this.items = items;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public RecommendationRun save(RecommendationRun run) {
        if (run.id() != null) {
            throw new IllegalArgumentException("Recommendation runs are immutable; only new runs can be saved");
        }
        ScoringResult result = run.result();
        RecommendationRunJpaEntity saved = runs.save(RecommendationRunJpaEntity.of(run.sessionId(),
                run.answersVersion(), result.modelVersion(), result.modelType(), run.datasetVersion(), run.engineUsed(),
                run.fallbackUsed(), result.minScore(), jsonMapper.writeValueAsString(result),
                Math.toIntExact(run.latencyMs()), now()));
        items.saveAll(run.items().stream()
                .map(item -> RecommendationItemJpaEntity.of(saved.getId(), item.rank(), item.breedUuid(),
                        item.compatibilityIndex()))
                .toList());
        return new RecommendationRun(saved.getId(), run.sessionId(), run.answersVersion(), run.datasetVersion(),
                run.engineUsed(), run.fallbackUsed(), run.latencyMs(), saved.getCreatedAt(), result, run.items());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecommendationRun> findLatestBySessionId(UUID sessionId) {
        return runs.findFirstBySessionIdOrderByCreatedAtDescIdDesc(sessionId).map(this::toDomain);
    }

    private RecommendationRun toDomain(RecommendationRunJpaEntity row) {
        ScoringResult result = jsonMapper.readValue(row.getResult(), ScoringResult.class);
        List<RecommendationRun.RankedBreed> ranked = items.findByRunIdOrderByRankAsc(row.getId()).stream()
                .map(item -> new RecommendationRun.RankedBreed(item.getRank(), item.getBreedId(),
                        result.recommendations().get(item.getRank() - 1).breedId(), item.getCompatibilityIndex()))
                .toList();
        return new RecommendationRun(row.getId(), row.getSessionId(), row.getAnswersVersion(),
                row.getDatasetVersion(), row.getEngineUsed(), row.isFallbackUsed(), row.getLatencyMs(),
                row.getCreatedAt(), result, ranked);
    }

    /** Microsecond precision, matching PostgreSQL timestamps. */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
