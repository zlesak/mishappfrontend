package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.QuizResultRepository;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizResultBackendServiceTest {

    private QuizResultRepository quizResultRepository;
    private QuizBackendService quizBackendService;
    private QuizAttemptService quizAttemptService;
    private QuizGradingService quizGradingService;
    private CurrentUserProvider currentUserProvider;
    private QuizResultBackendService service;

    @BeforeEach
    void setUp() {
        quizResultRepository = Mockito.mock(QuizResultRepository.class);
        quizBackendService = Mockito.mock(QuizBackendService.class);
        quizAttemptService = Mockito.mock(QuizAttemptService.class);
        quizGradingService = Mockito.mock(QuizGradingService.class);
        currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        Mockito.when(currentUserProvider.requireUserId()).thenReturn("bart");

        service = new QuizResultBackendService(
                quizResultRepository,
                quizBackendService,
                Mockito.mock(ChapterBackendService.class),
                quizAttemptService,
                quizGradingService,
                Mockito.mock(ListingQueries.class),
                currentUserProvider,
                Mockito.mock(AuditLog.class));
    }

    @Test
    void keepsTheAttemptWhenTheQuizCannotBeReadForGrading() {
        Mockito.when(quizBackendService.load("quiz-1", true))
                .thenThrow(new BackendException.NotFound("Kvíz s ID quiz-1 nebyl nalezen."));

        assertThatThrownBy(() -> service.submit(request()))
                .isInstanceOf(BackendException.NotFound.class);

        // The student submitted in time; a failure on our side must not cost them their attempt.
        Mockito.verify(quizAttemptService).requireInFlight("quiz-1");
        Mockito.verify(quizAttemptService, Mockito.never()).consume("quiz-1");
    }

    @Test
    void keepsTheAttemptWhenGradingFails() {
        Mockito.when(quizBackendService.load("quiz-1", true)).thenReturn(quiz());
        Mockito.when(quizGradingService.grade(Mockito.any(), Mockito.any()))
                .thenThrow(new IllegalStateException("grading blew up"));

        assertThatThrownBy(() -> service.submit(request()))
                .isInstanceOf(IllegalStateException.class);

        Mockito.verify(quizAttemptService, Mockito.never()).consume("quiz-1");
    }

    @Test
    void spendsTheAttemptOnlyOnceTheResultIsAboutToBeWritten() {
        Mockito.when(quizBackendService.load("quiz-1", true)).thenReturn(quiz());
        Mockito.when(quizGradingService.grade(Mockito.any(), Mockito.any()))
                .thenReturn(new QuizGradingService.Graded(1, 1, 100.0, List.of()));
        Mockito.when(quizResultRepository.save(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submit(request());

        InOrder inOrder = Mockito.inOrder(quizAttemptService, quizGradingService, quizResultRepository);
        inOrder.verify(quizAttemptService).requireInFlight("quiz-1");
        inOrder.verify(quizGradingService).grade(Mockito.any(), Mockito.any());
        inOrder.verify(quizAttemptService).consume("quiz-1");
        inOrder.verify(quizResultRepository).save(Mockito.any(QuizValidationResult.class));
    }

    @Test
    void refusesASubmissionWithNoAttemptBehindItBeforeReadingTheQuiz() {
        Mockito.doThrow(new BackendException.Validation("Kvíz nebyl řádně spuštěn, nelze jej odevzdat."))
                .when(quizAttemptService).requireInFlight("quiz-1");

        assertThatThrownBy(() -> service.submit(request()))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("nebyl řádně spuštěn");

        Mockito.verify(quizBackendService, Mockito.never()).load(Mockito.anyString(), Mockito.anyBoolean());
    }

    private QuizSubmissionRequest request() {
        return new QuizSubmissionRequest("quiz-1", List.of());
    }

    private QuizEntity quiz() {
        return QuizEntity.builder().id("quiz-1").name("Kvíz").questions(List.of()).answers(List.of()).build();
    }
}
