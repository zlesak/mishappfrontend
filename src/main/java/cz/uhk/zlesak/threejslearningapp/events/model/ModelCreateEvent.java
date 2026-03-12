package cz.uhk.zlesak.threejslearningapp.events.model;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * Event fired when a model should be created/uploaded.
 * This event is broadcast at the UI level to decouple the button from the upload logic.
 */
@Getter
public class ModelCreateEvent extends ComponentEvent<UI> {
    private final String modelName;

    public ModelCreateEvent(UI source, String modelName) {
        super(source, false);
        this.modelName = modelName;
    }
}

