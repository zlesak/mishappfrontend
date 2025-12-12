package cz.uhk.zlesak.threejslearningapp.events.quiz;

import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.components.common.ThreeJs;
import lombok.Getter;

/**
 * Event fired when a texture color is clicked in quiz mode.
 * Used for TEXTURE_CLICK type questions.
 */
@Getter
public class TextureClickedEvent extends ComponentEvent<ThreeJs> {
    private final String questionId;
    private final String modelId;
    private final String textureId;
    private final String hexColor;

    /**
     * Constructor for TextureClickedEvent.
     * @param source the ThreeJs component source
     * @param questionId the ID of the question
     * @param modelId the ID of the model
     * @param textureId the ID of the texture
     * @param hexColor the hexadecimal color code clicked
     */
    public TextureClickedEvent(ThreeJs source, String questionId, String modelId, String textureId, String hexColor) {
        super(source, false);
        this.questionId = questionId;
        this.modelId = modelId;
        this.textureId = textureId;
        this.hexColor = hexColor;
    }
}

