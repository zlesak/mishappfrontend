package cz.uhk.zlesak.threejslearningapp.components.quiz.questionRenderers;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.SingleChoiceSubmissionData;

import java.util.ArrayList;
import java.util.List;

/**
 * SingleChoiceQuestionRendererComponent Class - Renders a single-choice question using radio buttons.
 */
public class SingleChoiceQuestionRendererComponent extends AbstractQuestionRendererComponent {
    private final RadioButtonGroup<Integer> radioGroup;
    private final SingleChoiceQuestionData question;

    /**
     * Constructor for SingleChoiceQuestionRendererComponent.
     * @param question the single-choice question data to be rendered
     */
    SingleChoiceQuestionRendererComponent(SingleChoiceQuestionData question) {
        this.question = question;
        radioGroup = new RadioButtonGroup<>();
        radioGroup.setItems(getIndices(question.getOptions().size()));
        radioGroup.setItemLabelGenerator(i -> question.getOptions().get(i));
        radioGroup.addValueChangeListener(e -> {
            if (answerChangedListener != null) {
                answerChangedListener.accept(getSubmissionData());
            }
        });
        add(radioGroup);
    }

    /**
     * Gets the submission data for the single-choice question.
     * @return the submission data containing the selected index
     */
    @Override
    public  SingleChoiceSubmissionData getSubmissionData() {
        return SingleChoiceSubmissionData.builder()
                .questionId(question.getQuestionId())
                .type(question.getType())
                .selectedIndex(radioGroup.getValue() + 1) // +1 to convert from 0-based to 1-based index as answer is stored with 1-based index
                .build();
    }

    /**
     * Generates a list of indices from 0 to count-1.
     * @param count the number of indices to generate
     * @return the list of indices
     */
    private List<Integer> getIndices(int count) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            indices.add(i);
        }
        return indices;
    }
}