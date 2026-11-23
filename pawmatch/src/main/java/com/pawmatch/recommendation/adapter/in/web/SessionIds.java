package com.pawmatch.recommendation.adapter.in.web;

import java.util.UUID;

import com.pawmatch.common.kernel.ResourceNotFoundException;

/** Parses the session id path variable. */
final class SessionIds {

    private SessionIds() {
    }

    /** A malformed id can't match any session, so it gets the same 404 as an unknown one. */
    static UUID parse(String sessionId) {
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("questionnaire_session", sessionId);
        }
    }
}
