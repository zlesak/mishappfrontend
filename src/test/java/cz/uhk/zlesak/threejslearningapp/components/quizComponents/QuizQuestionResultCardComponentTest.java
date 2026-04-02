package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizQuestionResultCardComponentTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void shouldShowCorrectStatus_whenAnswerIsCorrect() {
        QuizQuestionResultCardComponent card =
                new QuizQuestionResultCardComponent(1, "What is bone?", true, 5);
        List<String> texts = spanTexts(card);
        assertTrue(texts.stream().anyMatch(t -> t.contains("Správně")),
                "Expected correct indicator for isCorrect=true");
    }

    @Test
    void shouldShowIncorrectStatus_whenAnswerIsWrong() {
        QuizQuestionResultCardComponent card =
                new QuizQuestionResultCardComponent(2, "What is cartilage?", false, 0);
        List<String> texts = spanTexts(card);
        assertTrue(texts.stream().anyMatch(t -> t.contains("Špatně")),
                "Expected incorrect indicator for isCorrect=false");
    }

    @Test
    void shouldShowUnfilledLabel_whenScoreIsMinusOne() {
        QuizQuestionResultCardComponent card =
                new QuizQuestionResultCardComponent(3, "Unfilled question?", false, -1);
        List<String> texts = spanTexts(card);
        assertTrue(texts.stream().anyMatch(t -> t.contains("Nevyplněno")),
                "Expected 'Nevyplněno' when score is -1");
    }

    @Test
    void shouldShowPointsLabel_whenScoreIsNonNegative() {
        QuizQuestionResultCardComponent card =
                new QuizQuestionResultCardComponent(4, "Points question?", true, 3);
        List<String> texts = spanTexts(card);
        assertTrue(texts.stream().anyMatch(t -> t.contains("bodů")),
                "Expected 'bodů' label when score >= 0");
    }

    @Test
    void shouldContainQuestionText() {
        QuizQuestionResultCardComponent card =
                new QuizQuestionResultCardComponent(1, "Sample question text", true, 2);
        List<String> texts = spanTexts(card);
        assertTrue(texts.contains("Sample question text"),
                "Expected question text to appear in card");
    }

    @Test
    void shouldConstruct_withScoreZero_andIncorrect() {
        assertDoesNotThrow(() ->
                new QuizQuestionResultCardComponent(5, "Zero score question", false, 0));
    }

    private List<String> spanTexts(QuizQuestionResultCardComponent card) {
        return VaadinTestSupport.findAll(card, Span.class).stream()
                .map(Span::getText)
                .toList();
    }
}

