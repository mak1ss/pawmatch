package com.pawmatch.questionnaire.adapter.out.persistence;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.pawmatch.questionnaire.application.port.out.QuestionnaireSessionRepository;
import com.pawmatch.questionnaire.domain.Answers;
import com.pawmatch.questionnaire.domain.AnswersParser;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link QuestionnaireSessionRepository} backed by {@code questionnaire_session} and {@code questionnaire_answers}.
 * Answers are stored in their canonical JSON form ({@link AnswersParser#toMap}) and re-validated when read.
 */
@Component
class QuestionnaireSessionPersistenceAdapter implements QuestionnaireSessionRepository {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final QuestionnaireSessionJpaRepository sessions;
    private final QuestionnaireAnswersJpaRepository answers;
    private final JsonMapper jsonMapper;

    QuestionnaireSessionPersistenceAdapter(QuestionnaireSessionJpaRepository sessions,
                                           QuestionnaireAnswersJpaRepository answers, JsonMapper jsonMapper) {
        this.sessions = sessions;
        this.answers = answers;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public SubmittedAnswers createSession(Answers submitted, String questionnaireVersion) {
        Instant now = now();
        QuestionnaireSessionJpaEntity session = sessions.save(QuestionnaireSessionJpaEntity.createdAt(now));
        return toDomain(answers.save(QuestionnaireAnswersJpaEntity.of(
                session.getId(), 1, questionnaireVersion, toJson(submitted), now)));
    }

    @Override
    @Transactional
    public Optional<SubmittedAnswers> appendAnswers(UUID sessionId, Answers submitted, String questionnaireVersion) {
        Optional<QuestionnaireSessionJpaEntity> locked = sessions.findByIdForUpdate(sessionId);
        if (locked.isEmpty()) {
            return Optional.empty();
        }
        Instant now = now();
        int nextVersion = answers.findFirstBySessionIdOrderByVersionDesc(sessionId)
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);
        locked.get().touch(now);
        return Optional.of(toDomain(answers.save(QuestionnaireAnswersJpaEntity.of(
                sessionId, nextVersion, questionnaireVersion, toJson(submitted), now))));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubmittedAnswers> findLatest(UUID sessionId) {
        return answers.findFirstBySessionIdOrderByVersionDesc(sessionId).map(this::toDomain);
    }

    private SubmittedAnswers toDomain(QuestionnaireAnswersJpaEntity row) {
        Answers parsed = AnswersParser.parse(jsonMapper.readValue(row.getAnswers(), MAP));
        return new SubmittedAnswers(row.getSessionId(), row.getVersion(), row.getQuestionnaireVersion(), parsed,
                row.getCreatedAt());
    }

    private String toJson(Answers submitted) {
        return jsonMapper.writeValueAsString(AnswersParser.toMap(submitted));
    }

    /** Microsecond precision, matching PostgreSQL timestamps, so values read back equal values written. */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
