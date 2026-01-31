package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizResultView;

/**
 * A list item representing a quiz for listigng purposes.
 */
public class QuizResultListItem extends AbstractListItem {
    /**
     * Constructs a QuizListItem for the given quiz.
     */
    public QuizResultListItem(QuickQuizResult result, boolean administrationView, String redirect) {
        super(true, administrationView, VaadinIcon.CHECK_SQUARE);
        HorizontalLayout quizName = new HorizontalLayout();
        Span maxScoreLabel = new Span(text("quiz.result.maxScore.label") + ": ");
        Span maxScore = new Span(String.valueOf(result.getMaxScore()));
        Span totalScoreLabel = new Span(text("quiz.result.totalScore.label") + ": ");
        Span totalScore = new Span(String.valueOf(result.getTotalScore()));
        Span percentageLabel = new Span(text("quiz.result.percentage.label") + ": ");
        Span percentage = new Span(String.format("%.2f%%", result.getPercentage()));


        maxScore.getStyle().set("font-weight", "600");
        totalScore.getStyle().set("font-weight", "600");
        percentage.getStyle().set("font-weight", "600");

        quizName.add(maxScoreLabel, maxScore, totalScoreLabel, totalScore, percentageLabel, percentage);
        details.add(quizName);


        setOpenButtonClickListener(e ->
                UI.getCurrent().navigate(QuizResultView.class,
                        new RouteParameters(new RouteParam("quizId", result.getId()), new RouteParam("back", redirect)))
        );
    }
}

