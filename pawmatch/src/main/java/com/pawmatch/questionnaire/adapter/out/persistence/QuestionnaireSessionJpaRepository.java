package com.pawmatch.questionnaire.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface QuestionnaireSessionJpaRepository extends JpaRepository<QuestionnaireSessionJpaEntity, UUID> {

    /** {@code SELECT ... FOR UPDATE}: serializes concurrent answer submissions for one session. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from QuestionnaireSessionJpaEntity s where s.id = :id")
    Optional<QuestionnaireSessionJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
