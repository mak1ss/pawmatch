package com.pawmatch.questionnaire.application.port.in;

import com.pawmatch.questionnaire.domain.QuestionnaireContract;

/** Returns the current questionnaire contract: field keys, types, ranges and allowed codes. */
public interface GetQuestionnaireUseCase {

    QuestionnaireContract getQuestionnaire();
}
