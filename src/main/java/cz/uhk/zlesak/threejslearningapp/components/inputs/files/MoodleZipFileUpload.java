package cz.uhk.zlesak.threejslearningapp.components.inputs.files;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import cz.uhk.zlesak.threejslearningapp.common.MoodleZipParser;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.InfoNotification;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * File upload component specifically for Moodle ZIP exports.
 * Parses the ZIP structure and loads the content into EditorJs.
 */
@Slf4j
public class MoodleZipFileUpload extends FileUpload implements I18nAware {

    @Getter
    private MoodleZipParser.ParsedChapter parsedChapter;

    /**
     * Constructor for MoodleZipFileUpload
     *
     * @param editorJs EditorJs component to load content into
     * @param uploadedButton Button to show after successful upload
     */
    public MoodleZipFileUpload(EditorJs editorJs, Button uploadedButton) {
        super(List.of(".zip", "application/zip"), true, false, false);

        setUploadButton(new Button(text("moodleZipUploadButton.label"), new Icon(VaadinIcon.UPLOAD)));

        setUploadListener((fileName, uploadedMultipartFile) -> {
            try {
                parsedChapter = MoodleZipParser.parseZip(uploadedMultipartFile.getInputStream());

                StringBuilder combinedHtml = new StringBuilder();

                for (MoodleZipParser.SubChapter subChapter : parsedChapter.getSubChapters()) {
                    combinedHtml.append("<h1>").append(subChapter.getTitle()).append("</h1>\n");

                    String htmlContent = subChapter.getHtmlContent();

                    for (Map.Entry<String, byte[]> imageEntry : subChapter.getImages().entrySet()) {
                        String imageName = imageEntry.getKey();
                        byte[] imageData = imageEntry.getValue();

                        String base64 = java.util.Base64.getEncoder().encodeToString(imageData);
                        String mimeType = getMimeType(imageName);
                        String dataUrl = "data:" + mimeType + ";base64," + base64;

                        htmlContent = htmlContent.replaceAll(
                            "(<img[^>]*src=['\"]([^'\"]*/)?" + java.util.regex.Pattern.quote(imageName) + "['\"])",
                            "$1 data-filename=\"" + imageName + "\""
                        );

                        htmlContent = htmlContent.replaceAll(
                            "src=['\"]([^'\"]*/)?" + java.util.regex.Pattern.quote(imageName) + "['\"]",
                            "src=\"" + dataUrl + "\""
                        );
                    }

                    combinedHtml.append(htmlContent).append("\n\n");
                }

                editorJs.loadMoodleHtml(combinedHtml.toString());

                uploadedButton.setText(fileName);
                uploadedButton.setIcon(new Icon(VaadinIcon.CHECK));
                uploadedButton.setEnabled(false);
                uploadedButton.setVisible(true);
                setVisible(false);

                new InfoNotification(text("moodleZipUpload.success"));

            } catch (Exception ex) {
                log.error("Error parsing Moodle ZIP file: {}", ex.getMessage(), ex);
                try {
                    new ErrorNotification(text("moodleZipUpload.error") + ": " + ex.getMessage());
                } catch (Exception notificationEx) {
                    log.warn("Could not display error notification: {}", notificationEx.getMessage());
                }
            }
        });

        addFileRejectedListener(e -> new ErrorNotification(text("moodleZipUpload.rejected") + ": " + e.getErrorMessage()));
    }

    /**
     * Get MIME type based on file extension
     */
    private static String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }
}
