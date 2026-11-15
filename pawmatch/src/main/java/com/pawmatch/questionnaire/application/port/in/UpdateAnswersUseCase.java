package com.pawmatch.questionnaire.application.port.in;

import java.util.Map;
import java.util.UUID;

import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;

/** Stores a new version of a session's answers (the user changed their answers). */
public interface UpdateAnswersUseCase {

    /**
     * @return the stored answers with the next version number
     * @throws RequestValidationException listing every invalid field
     * @throws ResourceNotFoundException  if the session doesn't exist
     */
    SubmittedAnswers updateAnswers(UUID sessionId, Map<String, Object> rawAnswers);
}
