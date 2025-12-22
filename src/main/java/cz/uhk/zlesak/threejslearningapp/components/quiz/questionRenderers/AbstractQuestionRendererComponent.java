package cz.uhk.zlesak.threejslearningapp.components.quiz.questionRenderers;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Setter;

import java.util.function.Consumer;

/**
 * Base renderer for quiz questions during quiz taking.
 * Creates input based on question type.
 */
@Setter
public abstract class AbstractQuestionRendererComponent extends VerticalLayout implements I18nAware {
    protected Consumer<AbstractSubmissionData> answerChangedListener;

    protected AbstractQuestionRendererComponent() {
        super();
        setSpacing(true);
        setPadding(false);
    }

    /**
     * Gets the current answer value.
     *
     * @return Current answer
     */
    public abstract AbstractSubmissionData getSubmissionData();

    /**
     * Factory method to create renderer for question type.
     *
     * @param question Question data
     * @return Appropriate renderer component
     */
    public static AbstractQuestionRendererComponent create(AbstractQuestionData question) {
        return switch (question.getType()) {
            case SINGLE_CHOICE -> new SingleChoiceQuestionRendererComponent((SingleChoiceQuestionData) question);
            case MULTIPLE_CHOICE -> new MultipleChoiceQuestionRendererComponent((MultipleChoiceQuestionData) question);
            case OPEN_TEXT -> new OpenTextQuestionRendererComponent((OpenTextQuestionData) question);
            case MATCHING -> new MatchingQuestionRendererComponent((MatchingQuestionData) question);
            case ORDERING -> new OrderingQuestionRendererComponent((OrderingQuestionData) question);
            case TEXTURE_CLICK -> new TextureClickQuestionRendererComponent((TextureClickQuestionData) question);
        };
    }
}

