package cz.uhk.zlesak.threejslearningapp.components.inputs.quizes;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;

import java.util.UUID;

/**
 * Abstract base class for quiz options.
 */
public abstract class AbstractOption extends HorizontalLayout implements I18nAware {
    @Getter
    protected final String questionId;
    private final String labelText;
    @Getter
    protected int index;
    protected String label;
    @Getter
    protected final Button removeOptionButton = new Button(text("quiz.option.remove"));

    /**
     * Constructor for AbstractOption.
     * @param index the index of the option
     * @param labelTextKey the key for the label text
     */
    public AbstractOption(int index, String labelTextKey) {
        this.index = index;
        this.labelText = text(labelTextKey);
        this.label = labelText + " " + index;
        this.questionId = UUID.randomUUID().toString();
        removeOptionButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
    }

    /**
     * Updates the option index and label.
     * @param index the new index
     */
    protected void updateIndex(int index) {
        this.index = index;
        this.label = labelText + " " + index;
    }
}
