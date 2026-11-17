package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PageHeader;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.QuizResultDetailCardComponent;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.QuizScoreCardComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component for displaying quiz results after submission.
 */
public class QuizResultContainer extends VerticalLayout implements I18nAware {

    private final PageHeader header;

    /**
     * Creates a quiz result component showing overall score and detailed results.
     * @param result Quiz validation result
     */
    public QuizResultContainer(QuizValidationResult result, QuizEntity quiz, int possibleScore) {
        super();
        setSpacing(true);
        setPadding(true);

        // The score goes into the header's live region as well as onto the score card: submitting
        // replaces the whole screen, and without an announcement someone listening is left with the
        // silence of a page that has changed under them.
        header = new PageHeader(text("quiz.result.title"), scoreSummary(result, possibleScore));
        header.getStyle().set("padding", "0");

        QuizScoreCardComponent scoreCard = new QuizScoreCardComponent(result, possibleScore);
        QuizResultDetailCardComponent detailsCard = new QuizResultDetailCardComponent(result, quiz);
        Scroller scroller = new Scroller(detailsCard);
        scroller.setWidthFull();
        scroller.setSizeFull();

        add(header, scoreCard, scroller);
        setWidthFull();
    }

    /**
     * Moves keyboard focus onto the result heading, so the next Tab continues from the result rather
     * than from a submit button that no longer exists.
     */
    public void focusHeading() {
        header.focusHeading();
    }

    private String scoreSummary(QuizValidationResult result, int possibleScore) {
        Integer achieved = result == null ? null : result.getTotalScore();
        if (achieved == null) {
            return null;
        }
        return text("quiz.result.score") + ": " + achieved + " / " + possibleScore;
    }
}
