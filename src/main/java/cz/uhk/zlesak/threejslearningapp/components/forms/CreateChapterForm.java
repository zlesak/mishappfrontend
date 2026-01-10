package cz.uhk.zlesak.threejslearningapp.components.forms;

import com.flowingcode.vaadin.addons.markdown.MarkdownEditor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.components.inputs.files.MarkdownFileUpload;
import cz.uhk.zlesak.threejslearningapp.components.inputs.files.MoodleZipFileUpload;
import cz.uhk.zlesak.threejslearningapp.components.inputs.files.FileUpload;
import cz.uhk.zlesak.threejslearningapp.components.buttons.CreateChapterButton;
import cz.uhk.zlesak.threejslearningapp.components.buttons.MarkdownToggleButton;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import lombok.Getter;

/**
 * A toolbar composition for creating chapters with markdown and Moodle import support.
 * Includes buttons for creating chapters, toggling markdown view, uploading markdown files, and importing Moodle ZIP exports.
 */
public class CreateChapterForm extends HorizontalLayout {
    @Getter
    private CreateChapterButton createChapterButton;

    @Getter
    private MoodleZipFileUpload moodleZipFileUpload;

    public CreateChapterForm(EditorJs editorjs, MarkdownEditor mdEditor) {
        super();
        createChapterButton = new CreateChapterButton();
        Button markdownToggleButton = new MarkdownToggleButton();

        Button mdUploadedButton = new Button();
        mdUploadedButton.setVisible(false);
        FileUpload getMdFileUpload = new MarkdownFileUpload(editorjs, mdEditor, mdUploadedButton);

        Button moodleUploadedButton = new Button();
        moodleUploadedButton.setVisible(false);
        moodleZipFileUpload = new MoodleZipFileUpload(editorjs, moodleUploadedButton);

        add(markdownToggleButton, mdUploadedButton, getMdFileUpload, moodleUploadedButton, moodleZipFileUpload, createChapterButton);
        setWidthFull();
        setSpacing(true);
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.STRETCH);
        setFlexGrow(0, markdownToggleButton);
        setFlexGrow(0, getMdFileUpload);
        setFlexGrow(0, moodleZipFileUpload);
        setFlexGrow(1, createChapterButton);
    }
}
