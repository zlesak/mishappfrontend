package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.QuestionOption;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Editor for single choice questions.
 */
public class SingleChoiceQuestionEditor extends QuestionEditorBase<QuestionOption> {

    private final Select<Integer> correctAnswerGroup;

    /**
     * Constructor for SingleChoiceQuestionEditor.
     */
    public SingleChoiceQuestionEditor() {
        super(QuestionTypeEnum.SINGLE_CHOICE);

        correctAnswerGroup = new Select<>();
        correctAnswerGroup.setHelperText(text("quiz.singleChoice.correctOption"));
        correctAnswerGroup.addThemeVariants(SelectVariant.LUMO_SMALL);
        correctAnswerGroup.setItemLabelGenerator(i -> text("quiz.option.label") + " " + i);

        actionsLayout.addComponentAtIndex(1, correctAnswerGroup);

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
     * Removes an option by its ID.
     * @param id the ID of the option to remove
     */
    @Override
    void removeOption(String id) {
        options.stream().filter(option -> option.getQuestionId().equals(id)).findFirst().ifPresent(field -> {
            optionsLayout.remove(field);
            options.remove(field);
            indices.remove(field.getIndex() - 1);
            updateCorrectAnswerGroup();
        });
    }

    /**
     * Updates the correct answer group.
     */
    @Override
    void updateCorrectAnswerGroup() {
        indices.clear();
        for (QuestionOption option : options) {
            int index = indices.stream().max(Integer::compareTo).orElse(0) + 1;
            indices.add(index);
            option.update(index);
        }

        correctAnswerGroup.setItems(indices);
    }

    /**
     * Gets the question data for SingleChoiceQuestion as SingleChoiceQuestionData.
     *
     * @return QuestionData object (SingleChoiceQuestionData)
     */
    @Override
    public AbstractQuestionData getQuestionData() {
        List<String> optionTexts = this.options.stream()
                .map(option -> option.getOptionField().getValue())
                .collect(Collectors.toList());

        return SingleChoiceQuestionData.builder()
                .questionId(questionId)
                .questionText(getQuestionText())
                .type(questionType)
                .points(getPoints())
                .options(optionTexts)
                .build();
    }

    /**
     * Gets the answer data for SingleChoiceQuestion as SingleChoiceAnswerData.
     *
     * @return AnswerData object (SingleChoiceAnswerData)
     */
    @Override
    public AbstractAnswerData getAnswerData() {
        Integer selected = correctAnswerGroup.getValue();

        return SingleChoiceAnswerData.builder()
                .questionId(questionId)
                .type(questionType)
                .correctIndex(selected)
                .build();
    }

    /**
     * Validates the question data.
     * Valid if question text is not empty, at least two options are present,
     * all options are non-empty, and one correct answer is selected.
     *
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
        return correctAnswerGroup.getValue() != null;
    }
}

