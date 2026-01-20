package cz.uhk.zlesak.threejslearningapp.components.quiz;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component displaying detailed results for each quiz question.
 */
public class QuizResultDetailCard extends VerticalLayout implements I18nAware {

    /**
     * Creates detail card with per-question results.
     * @param result Quiz validation result
     * @param quiz   Quiz entity containing questions
     */
    public QuizResultDetailCard(QuizValidationResult result, QuizEntity quiz) {
        super();
        addClassName("details-card");
        getStyle()
                .set("padding", "var(--lumo-space-m)");

        H3 detailsTitle = new H3(text("quiz.result.details"));
        setWidthFull();
        add(detailsTitle);

        int questionNumber = 1;
        for (AbstractQuestionData question : quiz.getQuestions()) {
            String text = question.getQuestionText();
            Boolean isCorrect = null;
            Integer score = null;

            if (result.getQuestionResults() != null && result.getQuestionResults().containsKey(text)) {
                isCorrect = result.getQuestionResults().get(text);
                score = result.getQuestionScores().get(text);
            }

            QuizQuestionResultCard questionResult;
            if (isCorrect != null) {
                questionResult = new QuizQuestionResultCard(questionNumber, text, isCorrect, score);
            } else {
                questionResult = new QuizQuestionResultCard(questionNumber, text, false, -1);
            }
            add(questionResult);
            questionNumber++;
        }
    }

}
