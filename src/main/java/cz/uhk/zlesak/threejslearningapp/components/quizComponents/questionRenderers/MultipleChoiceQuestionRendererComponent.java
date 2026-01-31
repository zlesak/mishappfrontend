package cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MultipleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.MultipleChoiceSubmissionData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                .selectedItems(checkboxGroup.getValue().stream().map(i -> i + 1).collect(Collectors.toList()))  // +1 to convert from 0-based to 1-based index as answer is stored with 1-based index
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