package cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
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
        // A text field normally tells the server its value only when it loses focus. An answer
        // being typed when the clock runs out never loses focus, so it was submitted as blank and
        // the student was told they had answered nothing. Sending shortly after each pause in
        // typing keeps the answer on the server while it is being written, which is what the
        // automatic submission has to work from.
        textArea.setValueChangeMode(ValueChangeMode.LAZY);
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
