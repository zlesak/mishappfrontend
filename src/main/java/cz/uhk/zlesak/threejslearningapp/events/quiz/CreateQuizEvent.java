package cz.uhk.zlesak.threejslearningapp.events.quiz;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;

/**
 * Event fired when a quiz creation is called form CreateQuizButton
 */
public class CreateQuizEvent extends ComponentEvent<UI> {
    public CreateQuizEvent(UI source) {
        super(source, false);
    }
}

