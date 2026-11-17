package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationQuestion;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

import java.util.HashMap;
import java.util.List;
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
        for (String questionText : questionTexts(result, quiz)) {
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

    /**
     * Decides which questions the breakdown lists, and in what order.
     *
     * <p>The quiz is used when it is still there, so the breakdown follows the quiz's own question
     * order even for questions the student skipped. When the quiz has since been deleted, the
     * stored result stands on its own: grading records every question of the quiz, skipped ones
     * included, in that same order, so the breakdown survives the quiz rather than disappearing
     * with it.
     *
     * @param result the stored result.
     * @param quiz   the quiz as it stands now, or {@code null} when it no longer exists.
     * @return the question texts to render, in display order.
     */
    private List<String> questionTexts(QuizValidationResult result, QuizEntity quiz) {
        if (quiz != null && quiz.getQuestions() != null) {
            return quiz.getQuestions().stream()
                    .map(AbstractQuestionData::getQuestionText)
                    .toList();
        }
        if (result == null || result.getQuestionResults() == null) {
            return List.of();
        }
        return result.getQuestionResults().stream()
                .map(QuizValidationQuestion::getQuestionText)
                .toList();
    }

}
