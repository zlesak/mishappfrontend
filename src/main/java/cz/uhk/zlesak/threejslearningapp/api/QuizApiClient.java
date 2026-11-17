package cz.uhk.zlesak.threejslearningapp.api;

import cz.uhk.zlesak.threejslearningapp.api.contracts.IQuizApiClient;
import cz.uhk.zlesak.threejslearningapp.backend.service.QuizAttemptService;
import cz.uhk.zlesak.threejslearningapp.backend.service.QuizBackendService;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Quiz access for the UI.
 */
@Component
@RequiredArgsConstructor
public class QuizApiClient implements IQuizApiClient {

    private final QuizBackendService quizBackendService;
    private final QuizAttemptService quizAttemptService;

    @Override
    public QuickQuizEntity create(QuizEntity entity) {
        return quizBackendService.create(entity);
    }

    /**
     * Reads a quiz without its answers. Teachers use {@link #readAll(String)} when they need the
     * answer key.
     */
    @Override
    public QuizEntity read(String id) {
        return quizBackendService.require(id);
    }

    @Override
    public QuickQuizEntity readQuick(String id) {
        return quizBackendService.require(id);
    }

    @Override
    public PageResult<QuickQuizEntity> readEntities(FilterParameters<QuizFilter> filterParameters) {
        return quizBackendService.list(filterParameters);
    }

    @Override
    public QuizEntity update(String id, QuizEntity entity) {
        entity.setId(id);
        return quizBackendService.update(entity);
    }

    @Override
    public boolean delete(String id) {
        quizBackendService.delete(id);
        return true;
    }

    @Override
    public QuizEntity readQuizStudent(String quizId) {
        QuizEntity quiz = quizBackendService.require(quizId);
        Instant deadline = quizAttemptService.start(quizId,
                quiz.getTimeLimit() == null ? 0 : quiz.getTimeLimit());
        // What the player counts down is the attempt's remaining time, not the quiz's limit. Those
        // are the same thing only on the first visit; after a reload the attempt has been running
        // for a while and the limit would promise time the server will not honour.
        quiz.setRemainingSeconds(remainingSecondsUntil(deadline));
        return quiz;
    }

    /**
     * @param deadline when the attempt runs out, or {@code null} for an untimed quiz.
     * @return whole seconds left, never negative; {@code null} when there is no deadline.
     */
    private static Integer remainingSecondsUntil(Instant deadline) {
        if (deadline == null) {
            return null;
        }
        long remaining = Duration.between(Instant.now(), deadline).toSeconds();
        return (int) Math.max(remaining, 0);
    }

    @Override
    public QuizEntity readAll(String quizId) {
        return quizBackendService.requireWithAnswers(quizId);
    }
}
