package cz.uhk.zlesak.threejslearningapp.components.inputs.quizes;

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;

/**
 * Represents a question option in a quiz.
 */
public class QuestionOption extends AbstractOption {
    @Getter
    TextField optionField = new TextField();

    /**
     * Constructor for QuestionOption.
     * @param index the index of the option
     * @param labelTextKey the key for the label text
     */
    public QuestionOption(int index, String labelTextKey) {
        super(index, labelTextKey);
        optionField.setLabel(this.label);
        optionField.setWidthFull();
        optionField.setPlaceholder(text("quiz.option.placeholder"));

        add(optionField, removeOptionButton);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.END);
    }

    /**
     * Updates the option with a new index.
     * @param index the new index
     */
    public void update(int index) {
        updateIndex(index);
        optionField.setLabel(this.label);
    }
}
