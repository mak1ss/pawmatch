package com.pawmatch.inference.baseline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.pawmatch.questionnaire.domain.AnswersParser;
import com.pawmatch.recommendation.application.service.ScoringInputs;
import com.pawmatch.recommendation.domain.ScoringProfile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** JSON helpers for comparing the Java engine with the Python reference output. */
final class ParityJson {

    /** Serializes engine results with the model's snake_case field names. */
    static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private ParityJson() {
    }

    /** A golden profile, parsed by the real questionnaire validator and mapped like the service does. */
    static ScoringProfile profile(JsonNode profileJson) {
        Map<String, Object> raw = MAPPER.convertValue(profileJson, new TypeReference<>() {
        });
        return ScoringInputs.profile(AnswersParser.parse(raw));
    }

    /** Applies the agreed differences to raw reference output: no per-breed provenance, excluded sorted by id. */
    static JsonNode toContract(JsonNode referenceOutput) {
        ObjectNode copy = (ObjectNode) referenceOutput.deepCopy();
        for (String key : List.of("recommendations", "breeds")) {
            if (copy.get(key) instanceof ArrayNode breeds) {
                breeds.forEach(b -> ((ObjectNode) b).remove(List.of("source_retrieved_at_utc", "dataset_version")));
            }
        }
        if (copy.get("excluded") instanceof ArrayNode excluded) {
            List<JsonNode> sorted = new ArrayList<>();
            excluded.forEach(sorted::add);
            sorted.sort((a, b) -> a.get("breed_id").asString().compareTo(b.get("breed_id").asString()));
            excluded.removeAll();
            sorted.forEach(excluded::add);
        }
        return copy;
    }

    /** Returns null if equal, otherwise the JSON path and values of the first difference. Numbers compare by value. */
    static String firstDifference(JsonNode expected, JsonNode actual, String path) {
        if (expected.isNumber() && actual.isNumber()) {
            return expected.decimalValue().compareTo(actual.decimalValue()) == 0 ? null
                    : path + ": expected " + expected + " but was " + actual;
        }
        if (expected.isObject() && actual.isObject()) {
            TreeSet<String> keys = new TreeSet<>();
            expected.propertyNames().forEach(keys::add);
            actual.propertyNames().forEach(keys::add);
            for (String key : keys) {
                if (!expected.has(key) || !actual.has(key)) {
                    return path + "." + key + ": " + (expected.has(key) ? "missing in actual" : "unexpected in actual");
                }
                String difference = firstDifference(expected.get(key), actual.get(key), path + "." + key);
                if (difference != null) {
                    return difference;
                }
            }
            return null;
        }
        if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                return path + ": expected " + expected.size() + " elements but was " + actual.size();
            }
            Iterator<JsonNode> e = expected.iterator();
            Iterator<JsonNode> a = actual.iterator();
            for (int i = 0; e.hasNext(); i++) {
                String difference = firstDifference(e.next(), a.next(), path + "[" + i + "]");
                if (difference != null) {
                    return difference;
                }
            }
            return null;
        }
        return expected.equals(actual) ? null : path + ": expected " + expected + " but was " + actual;
    }
}
