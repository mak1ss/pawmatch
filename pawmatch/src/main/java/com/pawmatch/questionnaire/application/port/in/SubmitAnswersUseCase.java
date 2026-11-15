package com.pawmatch.questionnaire.application.port.in;

import java.util.Map;

import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;

/** Starts a questionnaire session with its first answers. */
public interface SubmitAnswersUseCase {

    /**
     * @param rawAnswers JSON-shaped answers (maps, lists, strings, booleans, numbers)
     * @return the stored answers, version 1
     * @throws RequestValidationException listing every invalid field
     */
    SubmittedAnswers startSession(Map<String, Object> rawAnswers);
}
