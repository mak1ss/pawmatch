package com.pawmatch.inference.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.pawmatch.inference.engine.ModelEngine;
import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.ComparisonResult;
import com.pawmatch.recommendation.domain.EngineOutcome;
import com.pawmatch.recommendation.domain.ScoringProfile;
import com.pawmatch.recommendation.domain.ScoringResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class EngineRouterTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void primaryEngineAnswersWithoutFallback() {
        EngineRouter router = router(List.of(engine("baseline", false), engine("ml", false)), "baseline", "ml");

        EngineOutcome<ScoringResult> outcome = router.recommend(null, List.of(), 5, BigDecimal.TEN);

        assertThat(outcome.engineUsed()).isEqualTo("baseline");
        assertThat(outcome.fallbackUsed()).isFalse();
        assertThat(outcome.result().modelVersion()).isEqualTo("baseline-model");
        assertThat(fallbacks()).isZero();
    }

    @Test
    void failingPrimaryFallsBackAndIsCounted() {
        EngineRouter router = router(List.of(engine("ml", true), engine("baseline", false)), "ml", "baseline");

        EngineOutcome<ScoringResult> outcome = router.recommend(null, List.of(), 5, BigDecimal.TEN);

        assertThat(outcome.engineUsed()).isEqualTo("baseline");
        assertThat(outcome.fallbackUsed()).isTrue();
        assertThat(fallbacks()).isEqualTo(1.0);
        assertThat(router.compare(null, List.of()).fallbackUsed()).isTrue();
        assertThat(fallbacks()).isEqualTo(2.0);
    }

    @Test
    void failingPrimaryWithoutFallbackPropagates() {
        EngineRouter router = router(List.of(engine("baseline", true)), "baseline", "");

        assertThatThrownBy(() -> router.recommend(null, List.of(), 5, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class).hasMessage("baseline failed");
        assertThat(fallbacks()).isZero();
    }

    @Test
    void misconfiguredEnginesFailAtStartup() {
        assertThatIllegalStateException().isThrownBy(() -> router(List.of(engine("baseline", false)), "ml", ""))
                .withMessageContaining("Unknown scoring engine 'ml'");
        assertThatIllegalStateException()
                .isThrownBy(() -> router(List.of(engine("baseline", false)), "baseline", "baseline"))
                .withMessageContaining("must differ");
    }

    private EngineRouter router(List<ModelEngine> engines, String primary, String fallback) {
        return new EngineRouter(engines, registry, primary, fallback);
    }

    private double fallbacks() {
        return registry.counter(EngineRouter.FALLBACK_METRIC).count();
    }

    private static ModelEngine engine(String id, boolean failing) {
        return new ModelEngine() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public ScoringResult recommend(ScoringProfile profile, List<BreedFeatures> breeds, int limit,
                                           BigDecimal minScore) {
                if (failing) {
                    throw new IllegalStateException(id + " failed");
                }
                return new ScoringResult(id + "-model", "test", "", minScore, 0, 0, 0, List.of(), "", List.of(),
                        List.of());
            }

            @Override
            public ComparisonResult compare(ScoringProfile profile, List<BreedFeatures> breeds) {
                if (failing) {
                    throw new IllegalStateException(id + " failed");
                }
                return new ComparisonResult(id + "-model", List.of());
            }
        };
    }
}
