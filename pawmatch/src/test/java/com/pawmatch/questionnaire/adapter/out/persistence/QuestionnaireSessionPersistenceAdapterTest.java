package com.pawmatch.questionnaire.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.pawmatch.TestcontainersConfiguration;
import com.pawmatch.questionnaire.domain.Answers;
import com.pawmatch.questionnaire.domain.AnswersParser;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import com.pawmatch.questionnaire.domain.TestAnswers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs against PostgreSQL with the real migrations. Not transactional per test, because the concurrency test needs
 * real commits; each test deletes the sessions it created (the test database is shared).
 */
@Tag("docker")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, QuestionnaireSessionPersistenceAdapter.class,
        QuestionnaireSessionPersistenceAdapterTest.JsonConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionnaireSessionPersistenceAdapterTest {

    @Autowired
    QuestionnaireSessionPersistenceAdapter adapter;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;

    /** Sessions created by the current test. Only these are cleaned up: the test database is shared. */
    private final List<UUID> createdSessions = new CopyOnWriteArrayList<>();

    @AfterEach
    void cleanUp() {
        for (UUID sessionId : createdSessions) {
            jdbc.update("DELETE FROM questionnaire_answers WHERE session_id = ?", sessionId);
            jdbc.update("DELETE FROM questionnaire_session WHERE id = ?", sessionId);
        }
    }

    private SubmittedAnswers create(Answers answers, String questionnaireVersion) {
        SubmittedAnswers stored = adapter.createSession(answers, questionnaireVersion);
        createdSessions.add(stored.sessionId());
        return stored;
    }

    @Test
    void createsSessionWithVersionOneAndUuidV7() {
        SubmittedAnswers stored = create(TestAnswers.valid(), "1.0.0");

        assertThat(stored.version()).isEqualTo(1);
        assertThat(stored.sessionId().version()).isEqualTo(7);
        assertThat(adapter.findLatest(stored.sessionId())).contains(stored);
    }

    @Test
    void appendsIncreasingVersionsAndReturnsTheLatest() {
        UUID sessionId = create(TestAnswers.valid(), "1.0.0").sessionId();
        adapter.appendAnswers(sessionId, withActivity(4), "1.0.0");
        SubmittedAnswers third = adapter.appendAnswers(sessionId, withActivity(5), "1.0.0").orElseThrow();

        assertThat(third.version()).isEqualTo(3);
        assertThat(adapter.findLatest(sessionId).orElseThrow().answers().activityLevel()).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM questionnaire_answers WHERE session_id = ?",
                Integer.class, sessionId)).isEqualTo(3);
    }

    @Test
    void appendToUnknownSessionIsEmpty() {
        assertThat(adapter.appendAnswers(UUID.randomUUID(), TestAnswers.valid(), "1.0.0")).isEmpty();
        assertThat(adapter.findLatest(UUID.randomUUID())).isEmpty();
    }

    @Test
    void storesCanonicalJsonbAndRoundTripsOptionalFields() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("walk_minutes_daily", 45.5);
        raw.put("allowed_sizes", List.of("Small", "XSmall"));
        raw.put("hard_limits", Map.of("energy_level", 3));
        Answers answers = AnswersParser.parse(raw);

        UUID sessionId = create(answers, "1.0.0").sessionId();

        assertThat(adapter.findLatest(sessionId).orElseThrow().answers()).isEqualTo(answers);
        assertThat(jdbc.queryForObject("SELECT answers ->> 'preferred_size' FROM questionnaire_answers WHERE session_id = ?",
                String.class, sessionId)).isEqualTo("Small");
        assertThat(jdbc.queryForObject("SELECT jsonb_typeof(answers -> 'hard_limits') FROM questionnaire_answers WHERE session_id = ?",
                String.class, sessionId)).isEqualTo("object");
    }

    @Test
    void concurrentResubmitsGetDistinctVersions() throws Exception {
        UUID sessionId = create(TestAnswers.valid(), "1.0.0").sessionId();
        Callable<Integer> resubmit = () -> adapter.appendAnswers(sessionId, withActivity(4), "1.0.0").orElseThrow().version();

        List<Integer> versions;
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Future<Integer>> futures = pool.invokeAll(Collections.nCopies(8, resubmit));
            versions = futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        }

        assertThat(versions).containsExactlyInAnyOrder(2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void databaseRejectsDuplicateVersionAndInvalidRows() {
        UUID sessionId = create(TestAnswers.valid(), "1.0.0").sessionId();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> jdbc.update("""
                INSERT INTO questionnaire_answers (id, session_id, version, questionnaire_version, answers)
                VALUES (gen_random_uuid(), ?, 1, '1.0.0', '{}'::jsonb)""", sessionId)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_questionnaire_answers_session_version");
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> jdbc.update("""
                INSERT INTO questionnaire_answers (id, session_id, version, questionnaire_version, answers)
                VALUES (gen_random_uuid(), ?, 0, '1.0.0', '{}'::jsonb)""", sessionId)))
                .hasMessageContaining("ck_questionnaire_answers_version");
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> jdbc.update("""
                INSERT INTO questionnaire_answers (id, session_id, version, questionnaire_version, answers)
                VALUES (gen_random_uuid(), ?, 9, '1.0.0', '[1]'::jsonb)""", sessionId)))
                .hasMessageContaining("ck_questionnaire_answers_answers_object");
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> jdbc.update("""
                INSERT INTO questionnaire_answers (id, session_id, version, questionnaire_version, answers)
                VALUES (gen_random_uuid(), gen_random_uuid(), 1, '1.0.0', '{}'::jsonb)""")))
                .hasMessageContaining("fk_questionnaire_answers_session");
    }

    private static Answers withActivity(int level) {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("activity_level", level);
        return AnswersParser.parse(raw);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JsonConfig {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }
}
