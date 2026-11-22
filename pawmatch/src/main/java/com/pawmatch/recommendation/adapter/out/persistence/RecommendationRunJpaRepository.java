package com.pawmatch.recommendation.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecommendationRunJpaRepository extends JpaRepository<RecommendationRunJpaEntity, UUID> {

    Optional<RecommendationRunJpaEntity> findFirstBySessionIdOrderByCreatedAtDescIdDesc(UUID sessionId);
}
