package com.pawmatch.recommendation.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** Row of {@code recommendation_item}: one recommended breed of a run. */
@Entity
@Table(name = "recommendation_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RecommendationItemJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID runId;
    private int rank;
    private UUID breedId;

    @Column(precision = 4, scale = 1)
    private BigDecimal compatibilityIndex;

    static RecommendationItemJpaEntity of(UUID runId, int rank, UUID breedId, BigDecimal compatibilityIndex) {
        RecommendationItemJpaEntity entity = new RecommendationItemJpaEntity();
        entity.runId = runId;
        entity.rank = rank;
        entity.breedId = breedId;
        entity.compatibilityIndex = compatibilityIndex;
        return entity;
    }
}
