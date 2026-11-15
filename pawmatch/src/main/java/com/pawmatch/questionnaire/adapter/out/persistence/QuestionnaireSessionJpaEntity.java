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
import org.hibernate.annotations.UuidGenerator;

/** Row of {@code questionnaire_session}. */
@Entity
@Table(name = "questionnaire_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class QuestionnaireSessionJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private Instant createdAt;
    private Instant updatedAt;

    static QuestionnaireSessionJpaEntity createdAt(Instant now) {
        QuestionnaireSessionJpaEntity entity = new QuestionnaireSessionJpaEntity();
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    void touch(Instant now) {
        this.updatedAt = now;
    }
}
