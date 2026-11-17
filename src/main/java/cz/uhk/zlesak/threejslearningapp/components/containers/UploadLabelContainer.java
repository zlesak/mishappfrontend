package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.upload.Upload;

/**
 * A simple component that combines an upload with a label.
 */
public class UploadLabelContainer extends Div {
    /**
     * Constructor that initializes the container with an upload and a label.
     *
     * @param upload The upload to be included in the div; any {@link Upload}, so the combined drop zone
     *               is labelled the same way as the individual sections.
     * @param label The label text to be displayed in the div.
     */
    public UploadLabelContainer(Upload upload, String label) {
        super(label);
        setWidthFull();
        add(upload);
    }
}
