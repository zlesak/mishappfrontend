package cz.uhk.zlesak.threejslearningapp.components.quiz;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component for displaying quiz results after submission.
 */
public class QuizResultComponent extends VerticalLayout implements I18nAware {

    /**
     * Creates a quiz result component showing overall score and detailed results.
     * @param result Quiz validation result
     */
    public QuizResultComponent(QuizValidationResult result, QuizEntity quiz, int possibleScore) {
        super();
        setSpacing(true);
        setPadding(true);

        H2 title = new H2(text("quiz.result.title"));

        QuizScoreCard scoreCard = new QuizScoreCard(result, possibleScore);
        QuizResultDetailCard detailsCard = new QuizResultDetailCard(result, quiz);

        add(title, scoreCard, detailsCard);
        setWidthFull();
    }
}

