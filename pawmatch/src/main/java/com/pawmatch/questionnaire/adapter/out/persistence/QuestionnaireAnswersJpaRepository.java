package com.pawmatch.questionnaire.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface QuestionnaireAnswersJpaRepository extends JpaRepository<QuestionnaireAnswersJpaEntity, UUID> {

    Optional<QuestionnaireAnswersJpaEntity> findFirstBySessionIdOrderByVersionDesc(UUID sessionId);
}
