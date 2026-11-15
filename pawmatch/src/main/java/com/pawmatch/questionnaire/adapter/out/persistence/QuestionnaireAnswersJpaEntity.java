package com.pawmatch.questionnaire.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Row of {@code questionnaire_answers}: one version of a session's answers. Rows are never updated. */
@Entity
@Table(name = "questionnaire_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class QuestionnaireAnswersJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID sessionId;
    private int version;
    private String questionnaireVersion;

    /** Canonical answers JSON, stored as jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String answers;

    private Instant createdAt;

    static QuestionnaireAnswersJpaEntity of(UUID sessionId, int version, String questionnaireVersion, String answersJson,
                                            Instant now) {
        QuestionnaireAnswersJpaEntity entity = new QuestionnaireAnswersJpaEntity();
        entity.sessionId = sessionId;
        entity.version = version;
        entity.questionnaireVersion = questionnaireVersion;
        entity.answers = answersJson;
        entity.createdAt = now;
        return entity;
    }
}
