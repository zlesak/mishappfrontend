package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;

/**
 * A list item representing a quiz for listigng purposes.
 */
public class QuizResultListItem extends AbstractListItem {
    /**
     * Constructs a QuizListItem for the given quiz.
     */
    public QuizResultListItem(QuickQuizResult result, boolean administrationView) {
        super(true, administrationView);
        HorizontalLayout quizName = new HorizontalLayout();
        Span maxScoreLabel = new Span(text("quiz.result.maxScore.label") + ": ");
        Span maxScore = new Span(String.valueOf(result.getMaxScore()));
        Span totalScoreLabel = new Span(text("quiz.result.totalScore.label") + ": ");
        Span totalScore = new Span(String.valueOf(result.getTotalScore()));
        Span percentageLabel = new Span(text("quiz.result.percentage.label") + ": ");
        Span percentage = new Span(result.getPercentage() + "%");


        maxScore.getStyle().set("font-weight", "600");
        totalScore.getStyle().set("font-weight", "600");
        percentage.getStyle().set("font-weight", "600");

        quizName.add(maxScoreLabel, maxScore, totalScoreLabel, totalScore, percentageLabel, percentage);
        details.add(quizName);


        setOpenButtonClickListener(e -> UI.getCurrent().navigate("quiz-result/" + result.getId()));
    }
}

