package cz.uhk.zlesak.threejslearningapp.events.file;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * AbstractFileEvent is a base class for file-related events in the UI.
 * It extends ComponentEvent and includes information about the model ID, file type, and entity ID.
 */
@Getter
public abstract class AbstractFileEvent extends ComponentEvent<UI> {
    private final String modelId;
    private final FileType fileType;
    private final String entityId;
    private final String questionId;

    /**
     * @param source     The UI component firing the event.
     * @param modelId    ID of the model associated with the file.
     * @param fileType   Type of the file being handled.
     * @param entityId   ID of the entity the file belongs to.
     * @param fromClient Whether the event originated on the client side.
     * @param questionId ID of the quiz question associated with the file, if any.
     */
    public AbstractFileEvent(UI source, String modelId, FileType fileType, String entityId, boolean fromClient, String questionId) {
        super(source, fromClient);
        this.modelId = modelId;
        this.fileType = fileType;
        this.entityId = entityId;
        this.questionId = questionId;
    }

}
