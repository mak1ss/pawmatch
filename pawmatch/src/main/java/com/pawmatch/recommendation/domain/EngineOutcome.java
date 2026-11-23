package com.pawmatch.recommendation.domain;

import java.util.Objects;

/**
 * A scoring result plus which engine produced it.
 *
 * @param engineUsed   id of the engine that produced the result, e.g. {@code baseline}
 * @param fallbackUsed true if the primary engine failed and a fallback engine answered
 */
public record EngineOutcome<T>(T result, String engineUsed, boolean fallbackUsed) {

    public EngineOutcome {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(engineUsed, "engineUsed");
    }
}
