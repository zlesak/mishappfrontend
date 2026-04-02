package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizScoreCardComponentTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void shouldShowExcellentMessage_whenPercentageAtLeast90() {
        QuizScoreCardComponent card = new QuizScoreCardComponent(result(9, 10, 90.0), 10);
        List<String> texts = spanTexts(card);
        assertTrue(texts.contains("Výborně!"), "Expected 'Výborně!' for 90%");
    }

    @Test
    void shouldShowGoodMessage_whenPercentageBetween75And90() {
        QuizScoreCardComponent card = new QuizScoreCardComponent(result(8, 10, 80.0), 10);
        List<String> texts = spanTexts(card);
        assertTrue(texts.contains("Dobrá práce!"), "Expected 'Dobrá práce!' for 80%");
    }

    @Test
    void shouldShowPassedMessage_whenPercentageBetween60And75() {
        QuizScoreCardComponent card = new QuizScoreCardComponent(result(7, 10, 65.0), 10);
        List<String> texts = spanTexts(card);
        assertTrue(texts.contains("Prošel/a jste!"), "Expected 'Prošel/a jste!' for 65%");
    }

    @Test
    void shouldShowFailedMessage_whenPercentageBelow60() {
        QuizScoreCardComponent card = new QuizScoreCardComponent(result(5, 10, 50.0), 10);
        List<String> texts = spanTexts(card);
        assertTrue(texts.contains("Bohužel, zkuste to znovu."), "Expected failure message for 50%");
    }

    @Test
    void shouldConstruct_withExactly90Percent() {
        assertDoesNotThrow(() -> new QuizScoreCardComponent(result(9, 10, 90.0), 10));
    }

    @Test
    void shouldConstruct_withExactly75Percent() {
        assertDoesNotThrow(() -> new QuizScoreCardComponent(result(75, 100, 75.0), 100));
    }

    @Test
    void shouldConstruct_withExactly60Percent() {
        assertDoesNotThrow(() -> new QuizScoreCardComponent(result(6, 10, 60.0), 10));
    }

    private List<String> spanTexts(QuizScoreCardComponent card) {
        return VaadinTestSupport.findAll(card, Span.class).stream()
                .map(Span::getText)
                .toList();
    }

    private QuizValidationResult result(int totalScore, int maxScore, double percentage) {
        return QuizValidationResult.builder()
                .totalScore(totalScore)
                .maxScore(maxScore)
                .percentage(percentage)
                .build();
    }
}
