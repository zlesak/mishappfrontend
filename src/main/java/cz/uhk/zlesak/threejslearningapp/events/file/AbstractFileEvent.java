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

    public AbstractFileEvent(UI source, String modelId, FileType fileType, String entityId, boolean fromClient, String questionId) {
        super(source, fromClient);
        this.modelId = modelId;
        this.fileType = fileType;
        this.entityId = entityId;
        this.questionId = questionId;
    }

}
