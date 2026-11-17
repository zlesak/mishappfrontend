package cz.uhk.zlesak.threejslearningapp.api;

import cz.uhk.zlesak.threejslearningapp.backend.service.QuizAttemptService;
import cz.uhk.zlesak.threejslearningapp.backend.service.QuizBackendService;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers what the player is told about the clock. The attempt and the quiz disagree about how much
 * time is left as soon as a student comes back to a quiz they already started, and it is the
 * attempt that the server will enforce.
 */
class QuizApiClientTest {

    private QuizBackendService quizBackendService;
    private QuizAttemptService quizAttemptService;
    private QuizApiClient quizApiClient;

    @BeforeEach
    void setUp() {
        quizBackendService = Mockito.mock(QuizBackendService.class);
        quizAttemptService = Mockito.mock(QuizAttemptService.class);
        quizApiClient = new QuizApiClient(quizBackendService, quizAttemptService);
    }

    @Test
    void reportsTheTimeLeftOfAnAttemptThatIsAlreadyRunning() {
        quizIsRead(20);
        Mockito.when(quizAttemptService.start("quiz-1", 20))
                .thenReturn(Instant.now().plus(5, ChronoUnit.MINUTES));

        QuizEntity quiz = quizApiClient.readQuizStudent("quiz-1");

        // Five minutes, not the twenty the quiz allows.
        assertThat(quiz.getRemainingSeconds()).isBetween(295, 300);
        assertThat(quiz.getTimeLimit()).isEqualTo(20);
    }

    @Test
    void reportsTheFullLimitWhenTheAttemptStartsNow() {
        quizIsRead(20);
        Mockito.when(quizAttemptService.start("quiz-1", 20))
                .thenReturn(Instant.now().plus(20, ChronoUnit.MINUTES));

        QuizEntity quiz = quizApiClient.readQuizStudent("quiz-1");

        assertThat(quiz.getRemainingSeconds()).isBetween(1195, 1200);
    }

    @Test
    void reportsNoRemainingTimeForAnUntimedQuiz() {
        quizIsRead(null);
        Mockito.when(quizAttemptService.start("quiz-1", 0)).thenReturn(null);

        assertThat(quizApiClient.readQuizStudent("quiz-1").getRemainingSeconds()).isNull();
    }

    @Test
    void neverReportsNegativeTimeForAnAttemptThatHasRunOut() {
        quizIsRead(20);
        Mockito.when(quizAttemptService.start("quiz-1", 20))
                .thenReturn(Instant.now().minus(3, ChronoUnit.MINUTES));

        assertThat(quizApiClient.readQuizStudent("quiz-1").getRemainingSeconds()).isZero();
    }

    private void quizIsRead(Integer timeLimit) {
        Mockito.when(quizBackendService.require("quiz-1"))
                .thenReturn(QuizEntity.builder().id("quiz-1").name("Kvíz").timeLimit(timeLimit).build());
    }
}
