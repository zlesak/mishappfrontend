package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Marker written when a student starts a quiz and consumed when they submit it.
 * Submitting without a matching marker, or after {@link #deadline}, is rejected.
 * Keyed per user <em>and</em> quiz so starting a second quiz cannot clear the first one's timer;
 * a TTL index on {@link #startedAt} clears abandoned attempts.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = MongoCollections.QUIZ_ATTEMPT)
public class QuizAttemptDocument {

    @Id
    private String id;
    private String userId;
    private String quizId;
    private boolean hasTimeLimit;
    private Instant deadline;
    @Indexed(expireAfter = "P1D")
    private Instant startedAt;

    /**
     * Builds the composite key identifying one user's attempt at one quiz.
     *
     * @param userId id of the student.
     * @param quizId id of the quiz.
     * @return document id.
     */
    public static String key(String userId, String quizId) {
        return userId + ":" + quizId;
    }
}
