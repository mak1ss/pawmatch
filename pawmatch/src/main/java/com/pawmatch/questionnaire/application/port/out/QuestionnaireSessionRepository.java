package com.pawmatch.questionnaire.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.pawmatch.questionnaire.domain.Answers;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;

/** Stores questionnaire sessions and their versioned answers. */
public interface QuestionnaireSessionRepository {

    /** Creates a session and stores the answers as version 1. */
    SubmittedAnswers createSession(Answers answers, String questionnaireVersion);

    /**
     * Stores the answers as the session's next version. Safe under concurrent calls for the same session.
     *
     * @return the stored answers, or empty if the session doesn't exist
     */
    Optional<SubmittedAnswers> appendAnswers(UUID sessionId, Answers answers, String questionnaireVersion);

    Optional<SubmittedAnswers> findLatest(UUID sessionId);
}
