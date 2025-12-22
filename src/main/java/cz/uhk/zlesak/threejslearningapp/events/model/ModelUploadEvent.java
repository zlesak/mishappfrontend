package cz.uhk.zlesak.threejslearningapp.events.model;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * Event fired when a model is uploaded via ModelUploadForm.
 * This event is broadcast at the UI level to decouple the upload form from its consumers.
 */
@Getter
public class ModelUploadEvent extends ComponentEvent<UI> {
    private final String modelId;
    private final String model;
    private final String modelName;
    private final String texture;
    private final String textureName;
    private final boolean advanced;

    /**
     * Constructor for ModelUploadEvent.
     *
     * @param source        the UI that fired the event
     * @param model         the base64-encoded model data
     * @param texture       the base64-encoded texture data (can be null)
     * @param modelId       the model identifier
     * @param modelName     the model file name
     * @param textureName   the texture file name (can be null)
     */
    public ModelUploadEvent(UI source, String model, String texture, String modelId, String modelName, String textureName, boolean advanced) {
        super(source, false);
        this.model = model;
        this.texture = texture;
        this.modelId = modelId;
        this.modelName = modelName;
        this.textureName = textureName;
        this.advanced = advanced;
    }
}

