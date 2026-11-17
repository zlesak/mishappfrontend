package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.QuizAttemptDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tracks in-flight quiz attempts so a submission can be tied to a start and checked against the
 * time limit. Attempts are keyed per user and quiz, so starting one quiz never disturbs another,
 * and the marker is removed atomically on submission, which is what stops a single attempt from
 * being graded twice.
 */
@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final MongoTemplate mongoTemplate;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Records that the current user has started a quiz, or finds the attempt they already have.
     *
     * @param quizId    id of the quiz.
     * @param timeLimit minutes allowed; zero or less means no limit.
     * @return when the attempt runs out, or {@code null} when the quiz is untimed. This is the
     * deadline of the attempt that is actually in flight, so re-entering the player reports the
     * time left rather than the full limit.
     */
    public Instant start(String quizId, int timeLimit) {
        String userId = currentUserProvider.requireUserId();
        Instant now = Instant.now();

        // Insert-only: the player view starts an attempt every time it is entered, so a plain save
        // would let a student reset their own deadline by reloading the page. Writing every field
        // under $setOnInsert leaves an attempt that is already running exactly as it was, and
        // doing it as one findAndModify keeps that decision atomic rather than reading first and
        // racing with itself. The document comes back either way, which is what lets the caller
        // tell the student how long they have left rather than how long the quiz allows.
        QuizAttemptDocument attempt = mongoTemplate.findAndModify(
                byKey(userId, quizId),
                new Update()
                        .setOnInsert("userId", userId)
                        .setOnInsert("quizId", quizId)
                        .setOnInsert("hasTimeLimit", timeLimit > 0)
                        .setOnInsert("deadline", now.plus(Math.max(timeLimit, 0), ChronoUnit.MINUTES))
                        .setOnInsert("startedAt", now),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                QuizAttemptDocument.class);

        return attempt != null && attempt.isHasTimeLimit() ? attempt.getDeadline() : null;
    }

    /**
     * Checks that the current user has an attempt in flight for a quiz, leaving it in place.
     * Used to reject a submission before the quiz is read and graded, so that work is not done
     * for a submission that is going to be refused anyway.
     *
     * @param quizId id of the quiz being submitted.
     * @throws BackendException.Validation when the quiz was never started, was already submitted,
     *                                     or the time limit has passed.
     */
    public void requireInFlight(String quizId) {
        String userId = currentUserProvider.requireUserId();
        requireUsable(mongoTemplate.findOne(byKey(userId, quizId), QuizAttemptDocument.class));
    }

    /**
     * Consumes the attempt marker for a quiz.
     *
     * @param quizId id of the quiz being submitted.
     * @throws BackendException.Validation when the quiz was never started, was already submitted,
     *                                     or the time limit has passed.
     */
    public void consume(String quizId) {
        String userId = currentUserProvider.requireUserId();
        requireUsable(mongoTemplate.findAndRemove(byKey(userId, quizId), QuizAttemptDocument.class));
    }

    private void requireUsable(QuizAttemptDocument attempt) {
        if (attempt == null) {
            throw new BackendException.Validation("Kvíz nebyl řádně spuštěn, nelze jej odevzdat.");
        }
        if (attempt.isHasTimeLimit() && attempt.getDeadline() != null && attempt.getDeadline().isBefore(Instant.now())) {
            throw new BackendException.Validation("Časový limit kvízu vypršel.");
        }
    }

    private static Query byKey(String userId, String quizId) {
        return new Query(Criteria.where("_id").is(QuizAttemptDocument.key(userId, quizId)));
    }
}
