package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationQuestion;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

import java.util.HashMap;
import java.util.Map;

/**
 * Component displaying detailed results for each quiz question.
 */
public class QuizResultDetailCardComponent extends VerticalLayout implements I18nAware {

    /**
     * Creates detail card with per-question results.
     * Shows each question, whether it was answered correctly, and the score obtained.
     * If the question result is missing (was not answered by the user), it is marked as incorrect with zero score.
     * Also, the Map structure is used to speed up the lookup of question results and to not iterate through the list each time for all questions, if some were already found.
     * @param result Quiz validation result
     * @param quiz   Quiz entity containing questions
     */
    public QuizResultDetailCardComponent(QuizValidationResult result, QuizEntity quiz) {
        super();
        addClassName("details-card");
        getStyle()
                .set("padding", "var(--lumo-space-m)");

        H3 detailsTitle = new H3(text("quiz.result.details"));
        setWidthFull();
        add(detailsTitle);

        Map<String, QuizValidationQuestion> questionResultMap = new HashMap<>();
        if (result.getQuestionResults() != null) {
            for (QuizValidationQuestion questionResult : result.getQuestionResults()) {
                questionResultMap.put(questionResult.getQuestionText(), questionResult);
            }
        }

        int questionNumber = 1;
        for (AbstractQuestionData question : quiz.getQuestions()) {
            String questionText = question.getQuestionText();
            QuizValidationQuestion questionResult = questionResultMap.get(questionText);

            QuizQuestionResultCardComponent questionResultCard;
            if (questionResult != null) {
                questionResultCard = new QuizQuestionResultCardComponent(
                        questionNumber,
                        questionText,
                        questionResult.getIsCorrect(),
                        questionResult.getPoints()
                );
            } else {
                questionResultCard = new QuizQuestionResultCardComponent(questionNumber, questionText, false, 0);
            }
            add(questionResultCard);
            questionNumber++;
        }
    }

}
