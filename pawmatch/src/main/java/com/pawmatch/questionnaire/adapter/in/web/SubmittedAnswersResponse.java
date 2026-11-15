package com.pawmatch.questionnaire.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import com.pawmatch.questionnaire.domain.SubmittedAnswers;

/** Result of a submit or resubmit. */
record SubmittedAnswersResponse(UUID sessionId, int answersVersion, String questionnaireVersion, Instant submittedAt) {

    static SubmittedAnswersResponse from(SubmittedAnswers stored) {
        return new SubmittedAnswersResponse(stored.sessionId(), stored.version(), stored.questionnaireVersion(),
                stored.submittedAt());
    }
}
