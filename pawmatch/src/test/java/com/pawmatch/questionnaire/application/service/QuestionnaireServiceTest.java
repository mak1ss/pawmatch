package com.pawmatch.questionnaire.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.pawmatch.common.kernel.RequestValidationException;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import com.pawmatch.questionnaire.application.port.out.QuestionnaireSessionRepository;
import com.pawmatch.questionnaire.domain.Answers;
import com.pawmatch.questionnaire.domain.QuestionnaireContract;
import com.pawmatch.questionnaire.domain.SubmittedAnswers;
import com.pawmatch.questionnaire.domain.TestAnswers;
import org.junit.jupiter.api.Test;

class QuestionnaireServiceTest {

    private final InMemoryRepository repository = new InMemoryRepository();
    private final QuestionnaireService service = new QuestionnaireService(repository);

    @Test
    void startSessionStoresVersionOne() {
        SubmittedAnswers stored = service.startSession(TestAnswers.validRaw());

        assertThat(stored.version()).isEqualTo(1);
        assertThat(stored.questionnaireVersion()).isEqualTo(QuestionnaireContract.VERSION);
        assertThat(service.getLatestAnswers(stored.sessionId())).isEqualTo(stored);
    }

    @Test
    void updateStoresNextVersion() {
        UUID sessionId = service.startSession(TestAnswers.validRaw()).sessionId();
        Map<String, Object> changed = TestAnswers.validRaw();
        changed.put("activity_level", 5);

        SubmittedAnswers updated = service.updateAnswers(sessionId, changed);

        assertThat(updated.version()).isEqualTo(2);
        assertThat(service.getLatestAnswers(sessionId).answers().activityLevel()).isEqualTo(5);
    }

    @Test
    void invalidAnswersAreNotStored() {
        Map<String, Object> raw = TestAnswers.validRaw();
        raw.put("has_cats", 1);

        assertThatThrownBy(() -> service.startSession(raw)).isInstanceOf(RequestValidationException.class);
        assertThat(repository.rows).isEmpty();
    }

    @Test
    void unknownSessionIsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateAnswers(unknown, TestAnswers.validRaw()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getLatestAnswers(unknown)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static final class InMemoryRepository implements QuestionnaireSessionRepository {
        final Map<UUID, List<SubmittedAnswers>> rows = new HashMap<>();

        @Override
        public SubmittedAnswers createSession(Answers answers, String questionnaireVersion) {
            UUID id = UUID.randomUUID();
            SubmittedAnswers stored = new SubmittedAnswers(id, 1, questionnaireVersion, answers, Instant.now());
            rows.put(id, new ArrayList<>(List.of(stored)));
            return stored;
        }

        @Override
        public Optional<SubmittedAnswers> appendAnswers(UUID sessionId, Answers answers, String questionnaireVersion) {
            List<SubmittedAnswers> versions = rows.get(sessionId);
            if (versions == null) {
                return Optional.empty();
            }
            SubmittedAnswers stored = new SubmittedAnswers(sessionId, versions.size() + 1, questionnaireVersion,
                    answers, Instant.now());
            versions.add(stored);
            return Optional.of(stored);
        }

        @Override
        public Optional<SubmittedAnswers> findLatest(UUID sessionId) {
            return Optional.ofNullable(rows.get(sessionId)).map(List::getLast);
        }
    }
}
