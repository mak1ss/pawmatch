package com.pawmatch.questionnaire.adapter.in.web;

import com.pawmatch.questionnaire.application.port.in.GetQuestionnaireUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Questionnaire", description = "Machine-readable questionnaire contract")
@RestController
class QuestionnaireController {

    private final GetQuestionnaireUseCase getQuestionnaire;

    QuestionnaireController(GetQuestionnaireUseCase getQuestionnaire) {
        this.getQuestionnaire = getQuestionnaire;
    }

    @Operation(summary = "Get the questionnaire contract",
            description = "Field keys, types, ranges and allowed codes. Wording and layout are owned by the frontend.")
    @GetMapping("/api/v1/questionnaire")
    QuestionnaireResponse get() {
        return QuestionnaireResponse.from(getQuestionnaire.getQuestionnaire());
    }
}
