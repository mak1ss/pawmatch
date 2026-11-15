package com.pawmatch.questionnaire.adapter.in.web;

import java.util.List;
import java.util.Locale;

import com.pawmatch.questionnaire.domain.QuestionnaireContract;

/**
 * The questionnaire contract for the frontend: field keys, types, ranges and allowed codes. No wording: labels,
 * sections and ordering are owned by the frontend and mapped by {@code key} and option code.
 * Attributes that don't apply to a type (e.g. {@code min} on a boolean) are omitted.
 */
record QuestionnaireResponse(String version, List<FieldResponse> fields) {

    static QuestionnaireResponse from(QuestionnaireContract contract) {
        return new QuestionnaireResponse(contract.version(), contract.fields().stream().map(FieldResponse::from).toList());
    }

    /** {@code type} is one of: integer, number, boolean, choice, multi_choice, limits. */
    record FieldResponse(String key, String type, boolean required, Integer min, Integer max, List<String> options) {

        static FieldResponse from(QuestionnaireContract.Field field) {
            return new FieldResponse(field.key(), field.type().name().toLowerCase(Locale.ROOT), field.required(),
                    field.min(), field.max(), field.options().isEmpty() ? null : field.options());
        }
    }
}
