package com.pawmatch.recommendation.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecommendationItemJpaRepository extends JpaRepository<RecommendationItemJpaEntity, UUID> {

    List<RecommendationItemJpaEntity> findByRunIdOrderByRankAsc(UUID runId);
}
