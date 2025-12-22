package cz.uhk.zlesak.threejslearningapp.events.model;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import lombok.Getter;

/**
 * Event representing the loading of a model in the application.
 * This event is fired when a model is loaded into the view or context.
 * It carries the base64-encoded model and texture data or their URL to load directly from backend.
 */
@Getter
public class ModelLoadEvent extends ComponentEvent<Component> {
    private final String modelId;
    private final String model;
    private final String texture;
    private final String questionId;

    /**
     * Constructor for ModelLoadEvent.
     *
     * @param source        the source component that fired the event
     * @param model   model url or base64-encoded model data
     * @param texture texture url or base64-encoded texture data
     * @param modelId the identifier for the model
     * @param questionId the identifier for the associated question
     */
    public ModelLoadEvent(Component source, String model, String texture, String modelId, String questionId) {
        super(source, false);
        this.model = model;
        this.texture = texture;
        this.modelId = modelId;
        this.questionId = questionId;
    }

}

