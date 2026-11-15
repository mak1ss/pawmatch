package com.pawmatch.questionnaire.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One stored version of a session's answers.
 *
 * @param version              1 for the first submit, +1 for every resubmit
 * @param questionnaireVersion {@link QuestionnaireContract#VERSION} the answers were given against
 */
public record SubmittedAnswers(UUID sessionId, int version, String questionnaireVersion, Answers answers,
                               Instant submittedAt) {

    public SubmittedAnswers {
        Objects.requireNonNull(sessionId, "sessionId");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1, got " + version);
        }
        Objects.requireNonNull(questionnaireVersion, "questionnaireVersion");
        Objects.requireNonNull(answers, "answers");
        Objects.requireNonNull(submittedAt, "submittedAt");
    }
}
