package com.pawmatch.questionnaire.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/** Fixtures for questionnaire tests. {@link #validRaw()} is the baseline team's example_profile.json. */
public final class TestAnswers {

    private TestAnswers() {
    }

    /** A fresh, mutable, valid raw answers map. */
    public static Map<String, Object> validRaw() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("activity_level", 3);
        raw.put("shedding_tolerance", 2);
        raw.put("grooming_capacity", 2);
        raw.put("mental_stimulation_capacity", 3);
        raw.put("barking_tolerance", 2);
        raw.put("preferred_size", "Small");
        raw.put("has_young_children", true);
        raw.put("has_other_dogs", false);
        raw.put("has_cats", false);
        raw.put("housing_type", "apartment");
        raw.put("walk_minutes_daily", 60);
        raw.put("alone_hours_daily", 4);
        raw.put("owner_experience", "none");
        raw.put("training_minutes_daily", 20);
        return raw;
    }

    public static Answers valid() {
        return AnswersParser.parse(validRaw());
    }
}
