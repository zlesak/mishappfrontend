package cz.uhk.zlesak.threejslearningapp.components.quiz;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component displaying detailed results for each quiz question.
 */
public class QuizResultDetailCard extends VerticalLayout implements I18nAware {

    /**
     * Creates detail card with per-question results.
     * @param result Quiz validation result
     */
    public QuizResultDetailCard(QuizValidationResult result) {
        super();
        addClassName("details-card");
        getStyle()
                .set("padding", "var(--lumo-space-m)");

        H3 detailsTitle = new H3(text("quiz.result.details"));
        setWidthFull();
        add(detailsTitle);

        if (result.getQuestionResults() != null) {
            int questionNumber = 1;
            for (var entry : result.getQuestionResults().entrySet()) {
                String questionId = entry.getKey();
                Boolean isCorrect = entry.getValue();
                Integer score = result.getQuestionScores().get(questionId);

                QuizQuestionResultCard questionResult = new QuizQuestionResultCard(questionNumber, questionId, isCorrect, score);
                add(questionResult);
                questionNumber++;
            }
        }
    }

}
