package cz.uhk.zlesak.threejslearningapp.events.file;

import com.vaadin.flow.component.UI;

/**
 * RemoveFileEvent represents an event triggered when a file is removed from the model structure.
 * It extends AbstractFileEvent and includes information about the model ID, file type, and entity ID.
 */
public class RemoveFileEvent extends AbstractFileEvent{
    public RemoveFileEvent(UI source, String modelId, FileType fileType, String entityId) {
        super(source, modelId, fileType, entityId);
    }
}
