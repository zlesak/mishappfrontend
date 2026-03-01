package cz.uhk.zlesak.threejslearningapp.events.file;

import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * UploadFileEvent represents an event triggered when a file is uploaded to the model structure.
 * It extends AbstractFileEvent and includes information about the uploaded file in base64 format or its URL,
 * the file name, and whether it is the main model to display immediately.
 */
@Getter
public class UploadFileEvent extends AbstractFileEvent {
    private final String base64File;
    private final String fileName;
    private final boolean main;

    public UploadFileEvent(UI source, String modelId, FileType fileType, String entityId, String base64File, String fileName, boolean fromClient, String questionId, boolean... main) {
        super(source, modelId, fileType, entityId, fromClient, questionId);
        this.base64File = base64File;
        this.fileName = fileName;
        this.main = main.length > 0 && main[0];
    }
}
