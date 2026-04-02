package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.QuizApiClient;
import cz.uhk.zlesak.threejslearningapp.api.clients.QuizResultApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContextException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {
    private QuizApiClient quizApiClient;
    private QuizResultApiClient quizResultApiClient;
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizApiClient = mock(QuizApiClient.class);
        quizResultApiClient = mock(QuizResultApiClient.class);
        quizService = new QuizService(quizApiClient, quizResultApiClient);
    }

    @Test
    void saveQuiz_shouldCreateWithQuestionsAndAnswers() throws Exception {
        SingleChoiceQuestionData question = SingleChoiceQuestionData.builder()
                .questionId("q-1")
                .questionText("Question")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(5)
                .options(List.of("A", "B"))
                .build();
        SingleChoiceAnswerData answer = SingleChoiceAnswerData.builder()
                .questionId("q-1")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .correctIndex(1)
                .build();

        AtomicInteger questionCount = new AtomicInteger(-1);
        AtomicInteger answerCount = new AtomicInteger(-1);
        when(quizApiClient.create(any(QuizEntity.class))).thenAnswer(invocation -> {
            QuizEntity entity = invocation.getArgument(0);
            questionCount.set(entity.getQuestions().size());
            answerCount.set(entity.getAnswers().size());
            return cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity.builder().id("quiz-1").build();
        });

        String id = quizService.saveQuiz(
                null,
                false,
                null,
                "Quiz",
                "Description",
                30,
                "chapter-1",
                List.of(question),
                List.of(answer)
        );

        assertEquals("quiz-1", id);
        assertEquals(1, questionCount.get());
        assertEquals(1, answerCount.get());

        ArgumentCaptor<QuizEntity> captor = ArgumentCaptor.forClass(QuizEntity.class);
        verify(quizApiClient).create(captor.capture());
        assertEquals("Quiz", captor.getValue().getName());

        assertThrows(
                RuntimeException.class,
                () -> quizService.saveQuiz(null, false, null, "Quiz", "Description", 30, "chapter-1", null, null)
        );
    }

    @Test
    void saveQuiz_shouldThrowForEditModeWithoutLoadedQuiz() {
        assertThrows(
                ApplicationContextException.class,
                () -> quizService.saveQuiz(
                        "quiz-1", true, null, "Quiz", "Description", 30, "chapter-1", List.of(), List.of()
                )
        );
    }

    @Test
    void calculatePossibleScore_shouldSumQuestionPoints() throws Exception {
        QuizEntity quiz = QuizEntity.builder()
                .questions(List.of(
                        SingleChoiceQuestionData.builder().points(4).build(),
                        SingleChoiceQuestionData.builder().points(6).build()
                ))
                .build();
        when(quizApiClient.readQuizStudent("quiz-1")).thenReturn(quiz);

        int score = quizService.calculatePossibleScore("quiz-1");

        assertEquals(10, score);
    }

    @Test
    void validateAnswers_shouldDelegateToResultApiClient() throws Exception {
        QuizValidationResult expected = QuizValidationResult.builder().totalScore(7).build();
        AbstractSubmissionData submission = mock(AbstractSubmissionData.class);

        when(quizResultApiClient.validateAnswers(any(QuizSubmissionRequest.class))).thenReturn(expected);

        QuizValidationResult actual = quizService.validateAnswers("quiz-1", List.of(submission));

        assertSame(expected, actual);
    }

    @Test
    void saveQuiz_shouldUpdateInEditMode() throws Exception {
        QuizEntity loadedQuiz = QuizEntity.builder()
                .id("quiz-1")
                .creatorId("user-1")
                .created(Instant.parse("2024-01-01T00:00:00Z"))
                .questions(List.of())
                .answers(List.of())
                .build();
        SingleChoiceQuestionData question = SingleChoiceQuestionData.builder().points(1).build();
        SingleChoiceAnswerData answer = SingleChoiceAnswerData.builder().correctIndex(0).build();

        when(quizApiClient.update(any(String.class), any(QuizEntity.class)))
                .thenReturn(QuizEntity.builder().id("quiz-1").build());

        String id = quizService.saveQuiz(
                "quiz-1",
                true,
                loadedQuiz,
                "Updated",
                "Description",
                40,
                "chapter-1",
                List.of(question),
                List.of(answer)
        );

        assertEquals("quiz-1", id);
    }

    @Test
    void getQuizMethodsShouldWrapClientFailures() throws Exception {
        when(quizApiClient.readAll("quiz-1")).thenThrow(new RuntimeException("broken"));
        when(quizApiClient.readQuizStudent("quiz-1")).thenThrow(new RuntimeException("broken"));

        assertThrows(ApplicationContextException.class, () -> quizService.getQuizWithAnswers("quiz-1"));
        assertThrows(ApplicationContextException.class, () -> quizService.getQuizForStudent("quiz-1"));
    }

    @Test
    void saveQuizShouldClearBufferedQuestionsEvenWhenValidationFails() {
        SingleChoiceQuestionData question = SingleChoiceQuestionData.builder()
                .questionId("q-1")
                .questionText("Question")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(5)
                .options(List.of("A", "B"))
                .build();

        assertThrows(
                RuntimeException.class,
                () -> quizService.saveQuiz(null, false, null, "Quiz", "Description", 30, "chapter-1", List.of(question), List.of())
        );

        assertEquals(0, internalListSize("questions"));
        assertEquals(0, internalListSize("answers"));
    }

    private int internalListSize(String fieldName) {
        try {
            var field = QuizService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return ((List<?>) field.get(quizService)).size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
