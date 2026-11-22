package com.pawmatch.recommendation.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.pawmatch.TestcontainersConfiguration;
import com.pawmatch.recommendation.domain.RecommendationRun;
import com.pawmatch.recommendation.domain.ScoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** Runs against PostgreSQL with the real migrations: run/item round trip, foreign keys and checks. */
@Tag("docker")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class RecommendationRunPersistenceAdapterTest {

    @Autowired
    RecommendationRunJpaRepository runs;
    @Autowired
    RecommendationItemJpaRepository items;
    @Autowired
    JdbcTemplate jdbc;

    private RecommendationRunPersistenceAdapter adapter;
    private UUID sessionId;
    private UUID beagleId;

    @BeforeEach
    void setUp() {
        adapter = new RecommendationRunPersistenceAdapter(runs, items, JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build());
        sessionId = UUID.randomUUID();
        jdbc.update("INSERT INTO questionnaire_session (id) VALUES (?)", sessionId);
        jdbc.update("""
                INSERT INTO questionnaire_answers (id, session_id, version, questionnaire_version, answers)
                VALUES (gen_random_uuid(), ?, 1, '1.0.0', '{}'::jsonb)""", sessionId);
        beagleId = jdbc.queryForObject("SELECT id FROM breed WHERE slug = 'beagle'", UUID.class);
    }

    @Test
    void savesAndReadsBackTheLatestRunWithItems() {
        RecommendationRun first = adapter.save(run(1, "beagle", beagleId));
        RecommendationRun second = adapter.save(run(1, "beagle", beagleId));

        assertThat(first.id().version()).isEqualTo(7);
        RecommendationRun latest = adapter.findLatestBySessionId(sessionId).orElseThrow();
        assertThat(latest.id()).isEqualTo(second.id());
        assertThat(latest.result()).isEqualTo(second.result());
        assertThat(latest.items()).singleElement().satisfies(item -> {
            assertThat(item.breedId()).isEqualTo("beagle");
            assertThat(item.breedUuid()).isEqualTo(beagleId);
            assertThat(item.compatibilityIndex()).isEqualByComparingTo("72.5");
        });
        assertThat(adapter.findLatestBySessionId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void runMustReferenceAnExistingAnswersVersion() {
        assertThatThrownBy(() -> adapter.save(run(2, "beagle", beagleId)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_recommendation_run_answers");
    }

    @Test
    void itemMustReferenceAnExistingBreed() {
        assertThatThrownBy(() -> adapter.save(run(1, "ghost", UUID.randomUUID())))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_recommendation_item_breed");
    }

    private RecommendationRun run(int answersVersion, String slug, UUID breedUuid) {
        ScoringResult.ScoredBreed breed = new ScoringResult.ScoredBreed(slug, "Бігль", "Beagle", true, List.of(),
                new BigDecimal("72.5"), List.of(new ScoringResult.Factor("activity", "Активність", 4, 3,
                        new BigDecimal("0.7500"), 3.0, "Активність породи — 4/5, бажаний рівень — 3/5.")),
                List.of(), List.of("Активність породи — 4/5, бажаний рівень — 3/5."), "https://www.akc.org/");
        ScoringResult result = new ScoringResult("pawmatch-baseline-1.0.0", "weighted_rule_based_baseline", "s",
                BigDecimal.valueOf(60), 30, 30, 1, List.of(breed), "n", List.of("u"), List.of());
        return new RecommendationRun(null, sessionId, answersVersion, "1.0.0", "baseline", false, 2, null, result,
                List.of(new RecommendationRun.RankedBreed(1, breedUuid, slug, new BigDecimal("72.5"))));
    }
}
