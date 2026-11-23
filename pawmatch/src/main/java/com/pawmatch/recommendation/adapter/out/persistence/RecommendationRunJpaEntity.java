package com.pawmatch.recommendation.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
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

/** Row of {@code recommendation_run}. Rows are never updated. */
@Entity
@Table(name = "recommendation_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RecommendationRunJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID sessionId;
    private int answersVersion;
    private String modelVersion;
    private String modelType;
    private String datasetVersion;
    private String engineUsed;
    private boolean fallbackUsed;

    @Column(precision = 4, scale = 1)
    private BigDecimal minScore;

    /** The engine's full result as JSON, stored as jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String result;

    private int latencyMs;
    private Instant createdAt;

    static RecommendationRunJpaEntity of(UUID sessionId, int answersVersion, String modelVersion, String modelType,
                                         String datasetVersion, String engineUsed, boolean fallbackUsed,
                                         BigDecimal minScore, String resultJson, int latencyMs, Instant createdAt) {
        RecommendationRunJpaEntity entity = new RecommendationRunJpaEntity();
        entity.sessionId = sessionId;
        entity.answersVersion = answersVersion;
        entity.modelVersion = modelVersion;
        entity.modelType = modelType;
        entity.datasetVersion = datasetVersion;
        entity.engineUsed = engineUsed;
        entity.fallbackUsed = fallbackUsed;
        entity.minScore = minScore;
        entity.result = resultJson;
        entity.latencyMs = latencyMs;
        entity.createdAt = createdAt;
        return entity;
    }
}
