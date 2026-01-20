package cz.uhk.zlesak.threejslearningapp.components.quiz;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component displaying the overall quiz score.
 */
public class QuizScoreCard extends VerticalLayout implements I18nAware {

    /**
     * Creates score card displaying total score and percentage.
     *
     * @param result        Quiz validation result
     * @param possibleScore Possible maximum score
     */
    public QuizScoreCard(QuizValidationResult result, int possibleScore) {
        super();

        addClassName("score-card");
        addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Padding.LARGE, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.TextAlignment.CENTER,
                LumoUtility.Background.PRIMARY_10);

        H3 scoreTitle = new H3(text("quiz.result.score"));
        H1 scoreValue = new H1(result.getTotalScore() + " / " + possibleScore);
        scoreTitle.addClassNames(LumoUtility.TextColor.PRIMARY);

        Span percentage = new Span(String.format("%.2f%%", result.getPercentage()));
        percentage.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        String resultMessage = getResultMessage(result.getPercentage());
        Span message = new Span(resultMessage);
        message.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Top.MEDIUM);

        setSpacing(false);
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.CENTER);

        add(scoreTitle, scoreValue, percentage, message);
        setWidthFull();
    }

    /**
     * Generates a result message based on the percentage score.
     *
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
