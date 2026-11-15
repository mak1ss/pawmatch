package com.pawmatch.questionnaire.adapter.in.web;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.pawmatch.questionnaire.domain.AnswersParser;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;

/** Latest answers of a session, in the same snake_case shape the client submits. */
record SessionAnswersResponse(UUID sessionId, int answersVersion, String questionnaireVersion, Instant submittedAt,
                              Map<String, Object> answers) {

    static SessionAnswersResponse from(SubmittedAnswers stored) {
        return new SessionAnswersResponse(stored.sessionId(), stored.version(), stored.questionnaireVersion(),
                stored.submittedAt(), AnswersParser.toMap(stored.answers()));
    }
}
