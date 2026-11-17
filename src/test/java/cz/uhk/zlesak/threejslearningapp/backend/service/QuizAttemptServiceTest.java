package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.QuizAttemptDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizAttemptServiceTest {

    private MongoTemplate mongoTemplate;
    private QuizAttemptService quizAttemptService;

    @BeforeEach
    void setUp() {
        mongoTemplate = Mockito.mock(MongoTemplate.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        Mockito.when(currentUserProvider.requireUserId()).thenReturn("bart");
        quizAttemptService = new QuizAttemptService(mongoTemplate, currentUserProvider);
    }

    @Test
    void keepsAttemptsOfDifferentQuizzesApart() {
        quizAttemptService.start("quiz-1", 10);
        quizAttemptService.start("quiz-2", 5);

        assertThat(startedKeys()).containsExactly("bart:quiz-1", "bart:quiz-2");
    }

    @Test
    void recordsTheDeadlineForATimedQuiz() {
        Instant before = Instant.now();
        quizAttemptService.start("quiz-1", 15);

        Document inserted = insertedFields();
        assertThat(inserted.get("hasTimeLimit")).isEqualTo(true);
        assertThat((Instant) inserted.get("deadline")).isAfterOrEqualTo(before.plus(14, ChronoUnit.MINUTES));
    }

    @Test
    void marksAQuizWithoutALimitAsUntimed() {
        quizAttemptService.start("quiz-1", 0);

        assertThat(insertedFields().get("hasTimeLimit")).isEqualTo(false);
    }

    @Test
    void leavesARunningAttemptAloneWhenTheStudentReopensTheQuiz() {
        quizAttemptService.start("quiz-1", 15);

        // Every field is written under $setOnInsert and nothing is written outright, so reopening
        // the player finds the attempt already there and cannot push its deadline further out.
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        Mockito.verify(mongoTemplate).findAndModify(Mockito.any(Query.class), update.capture(),
                Mockito.any(FindAndModifyOptions.class), Mockito.eq(QuizAttemptDocument.class));
        Mockito.verify(mongoTemplate, Mockito.never()).save(Mockito.any(QuizAttemptDocument.class));

        assertThat(update.getValue().getUpdateObject().keySet()).containsExactly("$setOnInsert");
    }

    @Test
    void reportsTheDeadlineOfTheAttemptThatIsActuallyRunning() {
        // Started ten minutes ago with a fifteen minute limit: what comes back is the original
        // deadline, so the player counts down the five minutes left and not another fifteen.
        Instant runningDeadline = Instant.now().plus(5, ChronoUnit.MINUTES);
        attemptFound(QuizAttemptDocument.builder()
                .id("bart:quiz-1").hasTimeLimit(true).deadline(runningDeadline).build());

        assertThat(quizAttemptService.start("quiz-1", 15)).isEqualTo(runningDeadline);
    }

    @Test
    void reportsNoDeadlineForAQuizWithoutALimit() {
        attemptFound(QuizAttemptDocument.builder()
                .id("bart:quiz-1").hasTimeLimit(false).deadline(Instant.now()).build());

        assertThat(quizAttemptService.start("quiz-1", 0)).isNull();
    }

    @Test
    void leavesTheAttemptInPlaceWhileOnlyCheckingIt() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(QuizAttemptDocument.builder()
                        .id("bart:quiz-1").hasTimeLimit(true)
                        .deadline(Instant.now().plus(5, ChronoUnit.MINUTES)).build());

        quizAttemptService.requireInFlight("quiz-1");

        Mockito.verify(mongoTemplate, Mockito.never())
                .findAndRemove(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class));
    }

    @Test
    void refusesACheckForAQuizThatWasNeverStarted() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> quizAttemptService.requireInFlight("quiz-1"))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("nebyl řádně spuštěn");
    }

    @Test
    void refusesACheckThatArrivesAfterTheTimeLimit() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(QuizAttemptDocument.builder()
                        .id("bart:quiz-1").hasTimeLimit(true)
                        .deadline(Instant.now().minusSeconds(1)).build());

        assertThatThrownBy(() -> quizAttemptService.requireInFlight("quiz-1"))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("Časový limit");
    }

    private void attemptFound(QuizAttemptDocument attempt) {
        Mockito.when(mongoTemplate.findAndModify(Mockito.any(Query.class), Mockito.any(Update.class),
                        Mockito.any(FindAndModifyOptions.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(attempt);
    }

    private List<String> startedKeys() {
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        Mockito.verify(mongoTemplate, Mockito.atLeastOnce()).findAndModify(query.capture(),
                Mockito.any(Update.class), Mockito.any(FindAndModifyOptions.class),
                Mockito.eq(QuizAttemptDocument.class));
        return query.getAllValues().stream()
                .map(captured -> String.valueOf(captured.getQueryObject().get("_id")))
                .toList();
    }

    private Document insertedFields() {
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        Mockito.verify(mongoTemplate).findAndModify(Mockito.any(Query.class), update.capture(),
                Mockito.any(FindAndModifyOptions.class), Mockito.eq(QuizAttemptDocument.class));
        return (Document) update.getValue().getUpdateObject().get("$setOnInsert");
    }

    @Test
    void acceptsASubmissionThatFollowsAStart() {
        Mockito.when(mongoTemplate.findAndRemove(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(QuizAttemptDocument.builder()
                        .id("bart:quiz-1").hasTimeLimit(true)
                        .deadline(Instant.now().plus(5, ChronoUnit.MINUTES)).build());

        quizAttemptService.consume("quiz-1");
    }

    @Test
    void rejectsASubmissionForAQuizThatWasNeverStarted() {
        Mockito.when(mongoTemplate.findAndRemove(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> quizAttemptService.consume("quiz-1"))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("nebyl řádně spuštěn");
    }

    @Test
    void rejectsASecondSubmissionOfTheSameAttempt() {
        Mockito.when(mongoTemplate.findAndRemove(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(QuizAttemptDocument.builder().id("bart:quiz-1").build())
                .thenReturn(null);

        quizAttemptService.consume("quiz-1");

        assertThatThrownBy(() -> quizAttemptService.consume("quiz-1"))
                .isInstanceOf(BackendException.Validation.class);
    }

    @Test
    void rejectsASubmissionThatArrivesAfterTheTimeLimit() {
        Mockito.when(mongoTemplate.findAndRemove(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(QuizAttemptDocument.builder()
                        .id("bart:quiz-1").hasTimeLimit(true)
                        .deadline(Instant.now().minusSeconds(1)).build());

        assertThatThrownBy(() -> quizAttemptService.consume("quiz-1"))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("Časový limit");
    }

    @Test
    void acceptsALateSubmissionWhenTheQuizHasNoTimeLimit() {
        Mockito.when(mongoTemplate.findAndRemove(Mockito.any(Query.class), Mockito.eq(QuizAttemptDocument.class)))
                .thenReturn(QuizAttemptDocument.builder()
                        .id("bart:quiz-1").hasTimeLimit(false)
                        .deadline(Instant.now().minusSeconds(3600)).build());

        quizAttemptService.consume("quiz-1");
    }
}
