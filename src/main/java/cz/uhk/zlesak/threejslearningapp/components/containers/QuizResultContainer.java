package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.QuizResultDetailCardComponent;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.QuizScoreCardComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component for displaying quiz results after submission.
 */
public class QuizResultContainer extends VerticalLayout implements I18nAware {

    /**
     * Creates a quiz result component showing overall score and detailed results.
     * @param result Quiz validation result
     */
    public QuizResultContainer(QuizValidationResult result, QuizEntity quiz, int possibleScore) {
        super();
        setSpacing(true);
        setPadding(true);

        H2 title = new H2(text("quiz.result.title"));

        QuizScoreCardComponent scoreCard = new QuizScoreCardComponent(result, possibleScore);
        QuizResultDetailCardComponent detailsCard = new QuizResultDetailCardComponent(result, quiz);
        Scroller scroller = new Scroller(detailsCard);
        scroller.setWidthFull();
        scroller.setSizeFull();

        add(title, scoreCard, scroller);
        setWidthFull();
    }
}

