package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import cz.uhk.zlesak.threejslearningapp.events.quiz.CreateQuizEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Button for creating a new quiz
 * When clicked, it fires a CreateQuizEvent
 * Update mode can be set to change the button text to button.updateQuiz
 */
@Slf4j
public class CreateQuizButton extends AbstractButton<UI> {
    public CreateQuizButton() {
        super("createQuizButton.label", new CreateQuizEvent(UI.getCurrent()), null, ButtonVariant.LUMO_PRIMARY);
    }

    /**
     * Sets the button to update mode, changing the text to button.updateQuiz
     */
    public void setUpdateMode() {
        setText(text("button.updateQuiz"));
    }
}
