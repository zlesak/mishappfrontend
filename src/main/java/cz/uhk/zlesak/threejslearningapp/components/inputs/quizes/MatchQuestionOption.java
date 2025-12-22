package cz.uhk.zlesak.threejslearningapp.components.inputs.quizes;

import com.vaadin.flow.component.select.Select;
import lombok.Getter;

import java.util.List;

/**
 * Represents a match question option in a quiz.
 */
@Getter
public class MatchQuestionOption extends QuestionOption {
    Select<Integer> optionSelect = new Select<>();

    /**
     * Constructor for MatchQuestionOption.
     * @param index the index of the option
     * @param labelTextKey the key for the label text
     * @param optionIndices the list of option indices to select from
     */
    public MatchQuestionOption(int index, String labelTextKey, List<Integer> optionIndices) {
        super(index, labelTextKey);

        optionSelect.setItems(optionIndices);
        optionSelect.setLabel(text("quiz.option.label"));
        optionSelect.setItemLabelGenerator(i -> text("quiz.option.label") + " " + i);

        addComponentAtIndex(1, optionSelect);
    }

    /**
     * Updates the option with a new index and option indices.
     * @param index the new index
     * @param optionIndices the list of option indices to select from
     */
    public void update(int index, List<Integer> optionIndices) {
        Integer oldValue = optionSelect.getValue();
        optionSelect.setItems(optionIndices);
        if (optionIndices.contains(oldValue)) {
            optionSelect.setValue(oldValue);
        }
        updateIndex(index);
        optionField.setLabel(this.label);
    }
}
