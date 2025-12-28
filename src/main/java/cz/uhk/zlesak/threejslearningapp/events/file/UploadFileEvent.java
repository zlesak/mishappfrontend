package cz.uhk.zlesak.threejslearningapp.events.file;

import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * UploadFileEvent represents an event triggered when a file is uploaded to the model structure.
 * It extends AbstractFileEvent and includes information about the uploaded file in base64 format or its URL,
 * the file name, and whether it is an advanced upload.
 */
@Getter
public class UploadFileEvent extends AbstractFileEvent{
    private final String base64File;
    private final String fileName;
    private final boolean isAdvanced;

    public UploadFileEvent(UI source, String modelId, FileType fileType, String entityId, String base64File, String fileName, boolean... isAdvanced) {
        super(source, modelId, fileType, entityId);
        this.base64File = base64File;
        this.fileName = fileName;
        this.isAdvanced = isAdvanced.length > 0 && isAdvanced[0];

    }
}
