package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationQuestion;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QuizResultDetailCardComponentTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void shouldRender_whenQuestionResultsMatchQuestions() {
        QuizValidationQuestion qr = new QuizValidationQuestion("What is the femur?", true, 5, null);
        QuizValidationResult result = QuizValidationResult.builder()
                .totalScore(5)
                .maxScore(5)
                .percentage(100.0)
                .questionResults(List.of(qr))
                .build();

        QuizEntity quiz = quizWithQuestions(List.of("What is the femur?"));
        assertDoesNotThrow(() -> new QuizResultDetailCardComponent(result, quiz));
    }

    @Test
    void shouldRender_whenQuestionResultIsMissing() {
        QuizValidationResult result = QuizValidationResult.builder()
                .totalScore(0)
                .maxScore(5)
                .percentage(0.0)
                .questionResults(List.of())
                .build();

        QuizEntity quiz = quizWithQuestions(List.of("What is the tibia?"));
        assertDoesNotThrow(() -> new QuizResultDetailCardComponent(result, quiz));

        QuizResultDetailCardComponent card = new QuizResultDetailCardComponent(result, quiz);
        List<QuizQuestionResultCardComponent> questionCards =
                VaadinTestSupport.findAll(card, QuizQuestionResultCardComponent.class);
        assertFalse(questionCards.isEmpty(), "Expected a question result card for unanswered question");
    }

    @Test
    void shouldRender_whenQuestionResultsIsNull() {
        QuizValidationResult result = QuizValidationResult.builder()
                .totalScore(0)
                .maxScore(10)
                .percentage(0.0)
                .questionResults(null)
                .build();

        QuizEntity quiz = quizWithQuestions(List.of("What is the patella?"));
        assertDoesNotThrow(() -> new QuizResultDetailCardComponent(result, quiz));
    }

    @Test
    void shouldRender_multipleQuestions_withMixedResults() {
        QuizValidationQuestion matched = new QuizValidationQuestion("Question A", true, 3, null);
        QuizValidationResult result = QuizValidationResult.builder()
                .totalScore(3)
                .maxScore(6)
                .percentage(50.0)
                .questionResults(List.of(matched))
                .build();

        QuizEntity quiz = quizWithQuestions(List.of("Question A", "Question B"));
        QuizResultDetailCardComponent card = new QuizResultDetailCardComponent(result, quiz);

        List<QuizQuestionResultCardComponent> questionCards =
                VaadinTestSupport.findAll(card, QuizQuestionResultCardComponent.class);
        assertFalse(questionCards.isEmpty(), "Expected question result cards to be rendered");
    }

    @Test
    void shouldRender_withNoQuestions() {
        QuizValidationResult result = QuizValidationResult.builder()
                .totalScore(0)
                .maxScore(0)
                .percentage(0.0)
                .questionResults(List.of())
                .build();

        QuizEntity quiz = quizWithQuestions(List.of());
        assertDoesNotThrow(() -> new QuizResultDetailCardComponent(result, quiz));
    }

    private QuizEntity quizWithQuestions(List<String> questionTexts) {
        List<AbstractQuestionData> questions =
                questionTexts.stream()
                        .map(text -> (AbstractQuestionData)
                                SingleChoiceQuestionData.builder()
                                        .questionId("q-" + text.hashCode())
                                        .questionText(text)
                                        .points(5)
                                        .build())
                        .toList();

        return QuizEntity.builder()
                .id("quiz-1")
                .name("Test Quiz")
                .questions(questions)
                .build();
    }
}
