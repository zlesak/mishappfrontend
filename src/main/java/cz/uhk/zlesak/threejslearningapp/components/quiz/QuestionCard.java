package cz.uhk.zlesak.threejslearningapp.components.quiz;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.components.quiz.questionRenderers.AbstractQuestionRendererComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

import java.util.Map;

/**
 * Card component displaying a single quiz question during quiz taking.
 */
public class QuestionCard extends Div implements I18nAware {

    /**
     * Creates a question card component.
     * @param question question data
     * @param questionNumber number of the question
     * @param answers map to store user answers
     */
    public QuestionCard(AbstractQuestionData question, int questionNumber, Map<String, AbstractSubmissionData> answers) {
        super();
        setWidthFull();

        H3 questionTitle = new H3(text("quiz.question.number") + " " + questionNumber);
        Span questionText = new Span(question.getQuestionText());
        Span pointsLabel = new Span(text("quiz.question.points") + ": " + question.getPoints());
        pointsLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");

        AbstractQuestionRendererComponent renderer = AbstractQuestionRendererComponent.create(question);
        renderer.setAnswerChangedListener(answer -> answers.put(question.getQuestionId(), answer));

        VerticalLayout layout = new VerticalLayout(questionTitle, questionText, pointsLabel);
        layout.add(renderer);
        layout.setSpacing(true);
        layout.setPadding(false);

        add(layout);
    }
}
