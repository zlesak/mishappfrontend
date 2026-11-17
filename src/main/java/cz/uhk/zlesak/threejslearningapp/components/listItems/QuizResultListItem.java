package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.common.DateFormater;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizResultView;

/**
 * One quiz attempt in the attempt history.
 */
public class QuizResultListItem extends EntityRow {

    /**
     * Constructs the row for one attempt.
     *
     * @param result the attempt to show
     * @param administrationView whether to show edit and delete controls
     * @param redirect where the detail should return to
     */
    public QuizResultListItem(QuickQuizResult result, boolean administrationView, String redirect) {
        super(true, administrationView, VaadinIcon.CHECK_SQUARE);

        // The heading used to be left empty, so every attempt in the history was headed by the same
        // tick glyph and there was no way to tell one from another. The date and the score are what
        // distinguish them.
        setRowTitle(headingFor(result));

        // The attempt's own date is already the heading, so only the score, the percentage and the
        // chapter are added here; repeating the date would say the same thing twice.
        addMetadata(text("quiz.result.totalScore.label"), scoreOf(result));
        addMetadata(text("quiz.result.percentage.label"), result.getPercentage() == null
                ? null
                : String.format("%.2f%%", result.getPercentage()));
        addMetadata(text("quiz.chapter.label"), result.getChapterName());

        setOpenButtonClickListener(e ->
                UI.getCurrent().navigate(QuizResultView.class,
                        new RouteParameters(new RouteParam("quizId", result.getId()), new RouteParam("back", redirect)))
        );
    }

    /**
     * Names the attempt by when it was taken. Deliberately not "passed" or "failed": the result knows
     * only the score and the maximum, never a pass mark, so any verdict here would be invented.
     *
     * @param result the attempt
     * @return the heading text
     */
    private String headingFor(QuickQuizResult result) {
        if (result.getCreated() != null) {
            return text("quiz.result.attempt.heading", DateFormater.formatDate(result.getCreated()));
        }
        String score = scoreOf(result);
        return score == null ? text("quiz.result.attempt.unknownDate") : score;
    }

    /**
     * @param result the attempt
     * @return the score as "7 / 10", or {@code null} when it was not recorded
     */
    private String scoreOf(QuickQuizResult result) {
        if (result.getTotalScore() == null || result.getMaxScore() == null) {
            return null;
        }
        return result.getTotalScore() + " / " + result.getMaxScore();
    }
}
