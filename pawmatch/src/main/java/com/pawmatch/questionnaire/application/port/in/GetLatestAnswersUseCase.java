package com.pawmatch.questionnaire.application.port.in;

import java.util.UUID;

import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;

/**
 * Latest answers of a session. This is how other modules (recommendation, compare) read answers.
 */
public interface GetLatestAnswersUseCase {

    /**
     * @throws ResourceNotFoundException if the session doesn't exist
     */
    SubmittedAnswers getLatestAnswers(UUID sessionId);
}
