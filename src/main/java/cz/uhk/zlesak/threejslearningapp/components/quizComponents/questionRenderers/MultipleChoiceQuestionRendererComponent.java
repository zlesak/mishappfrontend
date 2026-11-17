package cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MultipleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.MultipleChoiceSubmissionData;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer for multiple choice questions during quiz taking.
 */
public class MultipleChoiceQuestionRendererComponent extends AbstractQuestionRendererComponent {
    private final CheckboxGroup<Integer> checkboxGroup;
    private final MultipleChoiceQuestionData question;

    /**
     * Constructor for MultipleChoiceQuestionRendererComponent.
     * @param question the multiple choice question data
     */
    MultipleChoiceQuestionRendererComponent(MultipleChoiceQuestionData question) {
        this.question = question;
        checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setItems(getIndices(question.getOptions().size()));
        checkboxGroup.setItemLabelGenerator(i -> question.getOptions().get(i));
        checkboxGroup.addValueChangeListener(e -> {
            if (answerChangedListener != null) {
                answerChangedListener.accept(getSubmissionData());
            }
        });
        add(checkboxGroup);
    }

    /**
     * Gets the current submission data.
     * @return Current multiple choice submission data
     */
    @Override
    public MultipleChoiceSubmissionData getSubmissionData() {
        return MultipleChoiceSubmissionData.builder()
                .questionId(question.getQuestionId())
                .type(question.getType())
                // The checkbox group is keyed by the option's own 0-based index, which is the
                // convention the stored answer key uses too, so the submission needs no shift.
                .selectedItems(new ArrayList<>(checkboxGroup.getValue()))
                .build();
    }

    /**
     * Generates a list of indices for the given count.
     * @param count the number of indices to generate
     * @return List of indices
     */
    private List<Integer> getIndices(int count) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            indices.add(i);
        }
        return indices;
    }
}