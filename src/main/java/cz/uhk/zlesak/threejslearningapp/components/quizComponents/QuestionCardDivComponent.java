package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.FieldSet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers.AbstractQuestionRendererComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

import java.util.Map;

/**
 * One question while the quiz is being taken.
 *
 * <p>A {@code <fieldset>} with a {@code <legend>}, because the answer controls are a group that only
 * makes sense together with the question. It was an H3, a loose Span with the wording, another Span
 * with the points and then the controls — so on a radio group a screen reader announced the text of
 * the chosen option and nothing about what was being asked.
 *
 * <p>The legend also says where in the quiz this is ("Otázka 6 z 12") and what it is worth. Knowing
 * how much is left is part of being able to finish.
 */
public class QuestionCardDivComponent extends FieldSet implements I18nAware {

    /** Prefix of the element id, so the submission summary can link to a question. */
    public static final String QUESTION_ID_PREFIX = "otazka-";

    /**
     * Creates a question card component.
     *
     * @param question question data
     * @param questionNumber number of the question, counted from one
     * @param questionCount how many questions the quiz has
     * @param answers map to store user answers
     * @param onAnswered notified whenever this question's answer changes, may be {@code null}
     */
    public QuestionCardDivComponent(AbstractQuestionData question, int questionNumber, int questionCount,
                                    Map<String, AbstractSubmissionData> answers, Runnable onAnswered) {
        super();
        setWidthFull();
        addClassName("quiz-question");
        setId(QUESTION_ID_PREFIX + questionNumber);
        // Focusable only from a link in the submission summary, never in the tab order itself.
        getElement().setAttribute("tabindex", "-1");
        getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin", "0");

        setLegendText(text("quiz.question.position", questionNumber, questionCount)
                + ": " + question.getQuestionText());
        getLegend().addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.MEDIUM);

        Span pointsLabel = new Span(text("quiz.question.points") + ": " + question.getPoints());
        pointsLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        AbstractQuestionRendererComponent renderer = AbstractQuestionRendererComponent.create(question);
        renderer.setAnswerChangedListener(answer -> {
            answers.put(question.getQuestionId(), answer);
            if (onAnswered != null) {
                onAnswered.run();
            }
        });

        VerticalLayout layout = new VerticalLayout(pointsLabel);
        layout.add(renderer);
        layout.setSpacing(true);
        layout.setPadding(false);

        add(layout);
    }

    /**
     * @return the question's wording, as shown in the legend.
     */
    public String getQuestionSummary() {
        return getLegendText();
    }
}
