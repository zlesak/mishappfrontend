package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.QuestionOption;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MultipleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MultipleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Editor for multiple choice questions.
 */
public class MultipleChoiceQuestionEditor extends QuestionEditorBase<QuestionOption> {
    private final MultiSelectComboBox<Integer> correctAnswersGroup;

    /**
     * Constructor for MultipleChoiceQuestionEditor.
     */
    public MultipleChoiceQuestionEditor() {
        super(QuestionTypeEnum.MULTIPLE_CHOICE);

        correctAnswersGroup = new MultiSelectComboBox<>();
        correctAnswersGroup.setHelperText(text("quiz.multipleChoice.correctOptions"));
        correctAnswersGroup.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        correctAnswersGroup.setItemLabelGenerator(i ->  text("quiz.option.label") + " " + i);

        actionsLayout.addComponentAtIndex(1, correctAnswersGroup);

        updateCorrectAnswerGroup();
    }

    /**
     * Creates a QuestionOption.
     *
     * @param index the index of the option
     * @return the created QuestionOption
     */
    @Override
    protected QuestionOption createOption(int index) {
        return new QuestionOption(index, "quiz.option.label");
    }

    /**
     * Updates the correct answers group.
     */
    @Override
    void updateCorrectAnswerGroup() {
        indices.clear();
        for (QuestionOption option : options) {
            int index = indices.stream().max(Integer::compareTo).orElse(0) + 1;
            indices.add(index);
            option.update(index);
        }

        correctAnswersGroup.setItems(indices);
    }

    /**
     * Gets the questi on data for MultipleChoiceQuestion as MultipleChoiceQuestionData.
     * @return MultipleChoiceQuestionData
     */
    @Override
    public AbstractQuestionData getQuestionData() {
        List<String> options = this.options.stream()
                .map(option -> option.getOptionField().getValue())
                .collect(Collectors.toList());

        return MultipleChoiceQuestionData.builder()
                .questionId(questionId)
                .questionText(getQuestionText())
                .type(questionType)
                .points(getPoints())
                .options(options)
                .build();
    }

    /**
     * Gets the answer data for MultipleChoiceQuestion as MultipleChoiceAnswerData.
     * @return MultipleChoiceAnswerData
     */
    @Override
    public AbstractAnswerData getAnswerData() {
        Set<Integer> selected = correctAnswersGroup.getValue();
        List<Integer> correctIndices = new ArrayList<>(selected);

        return MultipleChoiceAnswerData.builder()
                .questionId(questionId)
                .type(questionType)
                .correctItems(correctIndices)
                .build();
    }

    /**
     * Validates the question data.
     * @return true if valid, false otherwise
     */
    @Override
    public boolean validate() {
        if (getQuestionText() == null || getQuestionText().isEmpty()) {
            return false;
        }
        if (options.size() < 2) {
            return false;
        }
        for (QuestionOption field : options) {
            if (field.getOptionField().getValue() == null || field.getOptionField().getValue().isEmpty()) {
                return false;
            }
        }
        return !correctAnswersGroup.getValue().isEmpty();
    }
}

