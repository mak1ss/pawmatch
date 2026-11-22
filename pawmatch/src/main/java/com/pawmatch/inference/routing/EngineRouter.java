package com.pawmatch.inference.routing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pawmatch.inference.engine.ModelEngine;
import com.pawmatch.recommendation.application.port.out.ScoringEngine;
import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.EngineOutcome;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The {@link ScoringEngine} the application uses: calls the primary engine and, if it throws and a fallback is
 * configured, the fallback engine. Reports which engine answered and counts fallbacks in
 * {@code pawmatch.recommendation.fallback}.
 * <p>
 * Configuration: {@code pawmatch.inference.primary} (default {@code baseline}) and {@code pawmatch.inference.fallback}
 * (default: none). Unknown engine ids fail application startup.
 */
@Slf4j
@Component
class EngineRouter implements ScoringEngine {

    static final String FALLBACK_METRIC = "pawmatch.recommendation.fallback";

    private final ModelEngine primary;
    private final ModelEngine fallback;
    private final Counter fallbackCounter;

    EngineRouter(List<ModelEngine> engines, MeterRegistry meterRegistry,
                 @Value("${pawmatch.inference.primary:baseline}") String primaryId,
                 @Value("${pawmatch.inference.fallback:}") String fallbackId) {
        Map<String, ModelEngine> byId = engines.stream().collect(Collectors.toMap(ModelEngine::id, e -> e));
        this.primary = require(byId, primaryId);
        this.fallback = fallbackId.isBlank() ? null : require(byId, fallbackId);
        if (fallback == primary) {
            throw new IllegalStateException("pawmatch.inference.fallback must differ from the primary engine");
        }
        this.fallbackCounter = Counter.builder(FALLBACK_METRIC)
                .description("Recommendations answered by the fallback engine because the primary failed")
                .register(meterRegistry);
    }

    @Override
    public EngineOutcome<ScoringResult> recommend(ScoringProfile profile, List<BreedFeatures> breeds, int limit,
                                                  BigDecimal minScore) {
        return route(engine -> engine.recommend(profile, breeds, limit, minScore));
    }

    @Override
    public EngineOutcome<ComparisonResult> compare(ScoringProfile profile, List<BreedFeatures> breeds) {
        return route(engine -> engine.compare(profile, breeds));
    }

    private <T> EngineOutcome<T> route(Function<ModelEngine, T> call) {
        try {
            return new EngineOutcome<>(call.apply(primary), primary.id(), false);
        } catch (RuntimeException e) {
            if (fallback == null) {
                throw e;
            }
            log.warn("Engine '{}' failed, falling back to '{}'", primary.id(), fallback.id(), e);
            fallbackCounter.increment();
            return new EngineOutcome<>(call.apply(fallback), fallback.id(), true);
        }
    }

    private static ModelEngine require(Map<String, ModelEngine> byId, String id) {
        ModelEngine engine = byId.get(id);
        if (engine == null) {
            throw new IllegalStateException("Unknown scoring engine '" + id + "'; available: " + byId.keySet());
        }
        return engine;
    }
}
