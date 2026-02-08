package cz.uhk.zlesak.threejslearningapp.events.model;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.Getter;

/**
 * Event representing the loading of a model in the application.
 * This event is fired when a model is loaded into the view or context.
 * It carries the base64-encoded model and texture data or their URL to load directly from backend.
 */
@Getter
public class ModelLoadEvent extends ComponentEvent<Component> {
    private final QuickModelEntity quickModelEntity;
    private final String questionId;

    /**
     * Constructor for ModelLoadEvent.
     *
     * @param source        the source component that fired the event
     * @param quickModelEntity the quick model entity to load
     * @param questionId the identifier for the associated question
     */
    public ModelLoadEvent(Component source, QuickModelEntity quickModelEntity, String questionId) {
        super(source, false);
        this.quickModelEntity = quickModelEntity;
        this.questionId = questionId;
    }
}

