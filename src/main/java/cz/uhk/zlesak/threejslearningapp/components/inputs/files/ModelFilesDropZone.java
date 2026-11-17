package cz.uhk.zlesak.threejslearningapp.components.inputs.files;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * One drop area for every file a model is made of.
 *
 * <p>Uploading a model was the most expensive thing in the application: about 21 seconds and nine
 * interactions, roughly as much as the other seven measured tasks put together, because each kind of
 * file had its own button and its own file dialog — model, main texture, further textures, area map.
 * Everything can now be dropped at once and is sorted by extension.
 *
 * <p>The sections below it stay, so the sorting can be corrected and single files can still be
 * replaced or removed one at a time. This zone keeps nothing itself: each file is handed straight on
 * to the section it belongs to, and the zone empties again, so nothing is listed twice.
 */
public class ModelFilesDropZone extends Upload implements I18nAware {

    /** Everything a model consists of. */
    private static final List<String> ACCEPTED = List.of(".glb", ".obj", ".jpg", ".csv");

    /**
     * Constructs the drop zone.
     *
     * @param router receives each uploaded file with its name, and decides where it belongs
     */
    public ModelFilesDropZone(BiConsumer<String, InputStreamMultipartFile> router) {
        super();
        addClassName("model-files-dropzone");
        setAcceptedFileTypes(ACCEPTED.toArray(new String[0]));
        setMaxFileSize(50 * 1024 * 1024);
        setDropLabel(new Span(text("modelUploadForm.dropZone.label")));
        setUploadButton(new Button(text("modelUploadForm.dropZone.button")));

        setUploadHandler(UploadHandler.inMemory((metadata, data) -> {
            String fileName = metadata.fileName();
            router.accept(fileName, new InputStreamMultipartFile(new ByteArrayInputStream(data), fileName, null));
            // The file now lives in its own section; leaving a chip here as well would suggest it had
            // been uploaded twice.
            getElement().executeJs("this.files = []; this.requestContentUpdate();");
        }));

        addFileRejectedListener(event -> new ErrorNotification(event.getErrorMessage()));
    }
}
