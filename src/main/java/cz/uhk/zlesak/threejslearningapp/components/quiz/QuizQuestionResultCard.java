package cz.uhk.zlesak.threejslearningapp.components.quiz;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component displaying result for a single quiz question.
 */
public class QuizQuestionResultCard extends HorizontalLayout implements I18nAware {

    /**
     * Creates a question result card.
     * @param questionNumber number of the question
     * @param isCorrect whether the answer was correct
     * @param score score obtained for the question
     */
    public QuizQuestionResultCard(int questionNumber, Boolean isCorrect, Integer score) {
        super();
        setSpacing(true);
        setPadding(true);
        getStyle()
                .set("border-left", "3px solid " + (isCorrect ? "var(--lumo-success-color)" : "var(--lumo-error-color)"));

        Span questionLabel = new Span(text("quiz.question.number") + " " + questionNumber + ": ");
        questionLabel.getStyle().set("font-weight", "bold");

        Span statusLabel = new Span(isCorrect ? "✓ " + text("quiz.result.correct") : "✗ " + text("quiz.result.incorrect"));
        statusLabel.getStyle().set("color", isCorrect ? "var(--lumo-success-color)" : "var(--lumo-error-color)");

        Span scoreLabel = new Span(" (" + score + " " + text("quiz.result.points") + ")");
        scoreLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        add(questionLabel, statusLabel, scoreLabel);
    }
}
