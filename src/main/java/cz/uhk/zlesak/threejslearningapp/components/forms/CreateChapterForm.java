package cz.uhk.zlesak.threejslearningapp.components.forms;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.components.inputs.files.MoodleZipFileUpload;
import cz.uhk.zlesak.threejslearningapp.components.buttons.CreateChapterButton;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import lombok.Getter;

/**
 * CreateChapterForm - A form component for creating a new chapter.
 * It includes a button to create the chapter and a file upload component for uploading Moodle ZIP files
 */
public class CreateChapterForm extends HorizontalLayout {
    @Getter
    private CreateChapterButton createChapterButton;

    @Getter
    private MoodleZipFileUpload moodleZipFileUpload;

    /**
     * Constructor for CreateChapterForm.
     * @param editorjs the EditorJs component to be used for Moodle ZIP file upload processing
     */
    public CreateChapterForm(EditorJs editorjs) {
        super();
        createChapterButton = new CreateChapterButton();

        Button moodleUploadedButton = new Button();
        moodleUploadedButton.setVisible(false);
        moodleZipFileUpload = new MoodleZipFileUpload(editorjs, moodleUploadedButton);

        add(moodleUploadedButton, moodleZipFileUpload, createChapterButton);
        setWidthFull();
        setSpacing(true);
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.STRETCH);
        setFlexGrow(0, moodleZipFileUpload);
        setFlexGrow(1, createChapterButton);
    }
}
