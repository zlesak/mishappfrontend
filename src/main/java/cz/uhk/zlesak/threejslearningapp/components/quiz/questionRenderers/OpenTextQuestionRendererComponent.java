package cz.uhk.zlesak.threejslearningapp.components.quiz.questionRenderers;

import com.vaadin.flow.component.textfield.TextArea;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OpenTextSubmissionData;

/**
 * Renderer for open text questions during quiz taking.
 */
public class OpenTextQuestionRendererComponent extends AbstractQuestionRendererComponent {
    private final TextArea textArea;
    private final OpenTextQuestionData question;

    /**
     * Constructor for OpenTextQuestionRendererComponent.
     * @param question the open text question data
     */
    OpenTextQuestionRendererComponent(OpenTextQuestionData question) {
        this.question = question;
        textArea = new TextArea();
        textArea.setWidthFull();
        textArea.addValueChangeListener(e -> {
            if (answerChangedListener != null) {
                answerChangedListener.accept(getSubmissionData());
            }
        });
        add(textArea);
    }

    /**
     * Gets the current submission data.
     * @return Current open text submission data
     */
    @Override
    public OpenTextSubmissionData getSubmissionData() {
        return OpenTextSubmissionData.builder()
                .questionId(question.getQuestionId())
                .type(question.getType())
                .text(textArea.getValue())
                .build();
    }
}
