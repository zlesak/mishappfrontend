package cz.uhk.zlesak.threejslearningapp.components.quiz;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component displaying the overall quiz score.
 */
public class QuizScoreCard extends VerticalLayout implements I18nAware {

    /**
     * Creates score card displaying total score and percentage.
     * @param result Quiz validation result
     */
    public QuizScoreCard(QuizValidationResult result) {
        super();
        addClassName("score-card");
        getStyle()
                .set("border", "2px solid var(--lumo-primary-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("text-align", "center")
                .set("background", "var(--lumo-primary-color-10pct)");

        H3 scoreTitle = new H3(text("quiz.result.score"));
        H1 scoreValue = new H1(result.getTotalScore() + " / " + result.getMaxScore());
        scoreValue.getStyle().set("color", "var(--lumo-primary-color)");

        Span percentage = new Span(String.format("%.1f%%", result.getPercentage() * 100));
        percentage.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold");

        String resultMessage = getResultMessage(result.getPercentage() * 100);
        Span message = new Span(resultMessage);
        message.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-top", "var(--lumo-space-m)");
        setSpacing(false);
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.CENTER);

        add(scoreTitle, scoreValue, percentage, message);
        setWidthFull();
    }

    /**
     * Generates a result message based on the percentage score.
     * @param percentage Percentage score
     * @return Result message
     */
    private String getResultMessage(Double percentage) {
        if (percentage >= 90) {
            return text("quiz.result.excellent");
        } else if (percentage >= 75) {
            return text("quiz.result.good");
        } else if (percentage >= 60) {
            return text("quiz.result.passed");
        } else {
            return text("quiz.result.failed");
        }
    }
}
