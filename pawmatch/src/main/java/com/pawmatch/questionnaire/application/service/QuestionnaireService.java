package com.pawmatch.questionnaire.application.service;

import java.util.Map;
import java.util.UUID;

import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.application.port.in.GetLatestAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.GetQuestionnaireUseCase;
import com.pawmatch.questionnaire.application.port.in.SubmitAnswersUseCase;
import com.pawmatch.questionnaire.application.port.in.UpdateAnswersUseCase;
import com.pawmatch.questionnaire.application.port.out.QuestionnaireSessionRepository;
import com.pawmatch.questionnaire.domain.Answers;
import com.pawmatch.questionnaire.domain.AnswersParser;
import com.pawmatch.questionnaire.domain.QuestionnaireContract;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import org.springframework.stereotype.Service;

@Service
class QuestionnaireService
        implements GetQuestionnaireUseCase, SubmitAnswersUseCase, UpdateAnswersUseCase, GetLatestAnswersUseCase {

    static final String SESSION = "questionnaire_session";

    private final QuestionnaireSessionRepository repository;

    QuestionnaireService(QuestionnaireSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public QuestionnaireContract getQuestionnaire() {
        return QuestionnaireContract.current();
    }

    @Override
    public SubmittedAnswers startSession(Map<String, Object> rawAnswers) {
        Answers answers = AnswersParser.parse(rawAnswers);
        return repository.createSession(answers, QuestionnaireContract.VERSION);
    }

    @Override
    public SubmittedAnswers updateAnswers(UUID sessionId, Map<String, Object> rawAnswers) {
        Answers answers = AnswersParser.parse(rawAnswers);
        return repository.appendAnswers(sessionId, answers, QuestionnaireContract.VERSION)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION, sessionId.toString()));
    }

    @Override
    public SubmittedAnswers getLatestAnswers(UUID sessionId) {
        return repository.findLatest(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION, sessionId.toString()));
    }
}
