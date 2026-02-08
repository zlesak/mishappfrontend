package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * ThreeJsActionEvent represents an event triggered when a specific action is performed on a 3D model or texture.
 * It extends ComponentEvent and includes information about the model ID, texture ID, mask color, and the action performed.
 */
@Getter
public class ThreeJsActionEvent extends ComponentEvent<UI> {
    private final String modelId;
    private final String textureId;
    private final String maskColor;
    private final String questionId;
    private final ThreeJsActions action;
    private final boolean forceClient;

    public ThreeJsActionEvent(UI source, String modelId, String textureId, ThreeJsActions action, boolean fromClient, String questionId, String... maskColor) {
        super(source, fromClient);
        this.modelId = modelId;
        this.textureId = textureId;
        this.questionId = questionId;
        this.action = action;
        this.maskColor = maskColor.length > 0 ? maskColor[0] : null;
        this.forceClient = maskColor.length > 1;
    }
}

