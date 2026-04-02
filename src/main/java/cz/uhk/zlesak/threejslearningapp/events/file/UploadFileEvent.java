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

    /**
     * @param source     The UI component firing the event.
     * @param modelId    ID of the model the file is being uploaded to.
     * @param fileType   Type of the uploaded file.
     * @param entityId   ID of the entity the file belongs to.
     * @param base64File Base64-encoded file content, or a backend URL.
     * @param fileName   Original name of the uploaded file.
     * @param fromClient Whether the event originated on the client side.
     * @param questionId ID of the quiz question associated with the upload, if any.
     * @param main       Optional flag; when {@code true}, the file is the main model to display immediately.
     */
    public UploadFileEvent(UI source, String modelId, FileType fileType, String entityId, String base64File, String fileName, boolean fromClient, String questionId, boolean... main) {
        super(source, modelId, fileType, entityId, fromClient, questionId);
        this.base64File = base64File;
        this.fileName = fileName;
        this.main = main.length > 0 && main[0];
    }
}
