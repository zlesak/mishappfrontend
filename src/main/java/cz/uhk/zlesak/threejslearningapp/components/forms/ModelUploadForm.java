package cz.uhk.zlesak.threejslearningapp.components.forms;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.textfield.TextField;
import cz.uhk.zlesak.threejslearningapp.components.buttons.CreateModelButton;
import cz.uhk.zlesak.threejslearningapp.components.containers.UploadLabelContainer;
import cz.uhk.zlesak.threejslearningapp.components.inputs.files.FileUpload;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.NameTextField;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ModelUploadForm is a custom Vaadin component for uploading 3D models and their associated textures.
 * Both GLB and OBJ formats are accepted. Textures (main, other, CSV) are always optional and
 * visible regardless of file type – the loader is selected on the Three.js side from the file extension.
 */
@Slf4j
public class ModelUploadForm extends Scroller implements I18nAware {
    @Getter
    protected final VerticalLayout vl = new VerticalLayout();
    @Getter
    protected final TextField modelName;
    protected final UploadLabelContainer uploadModelDiv, uploadMainTextureDiv, uploadOtherTexturesDiv, csvOtherTexturesDiv;
    @Getter
    protected final FileUpload objFileUpload, mainTextureFileUpload, otherTexturesFileUpload, csvFileUpload;
    private final Map<String, QuickTextureEntity> quickTextureEntityMap = new HashMap<>();
    private final Map<String, String> csvMap = new HashMap<>();
    protected String modelUrl = null;
    protected String textureUrl = null;
    private String textureName = null;
    private String modelFileName = null;
    protected List<String> otherTexturesUrls = new ArrayList<>();
    protected List<String> csvBase64 = new ArrayList<>();
    protected final CreateModelButton createButton;

    /**
     * Constructor for ModelUploadForm.
     */
    public ModelUploadForm() {
        super(Scroller.ScrollDirection.VERTICAL);
        setContent(vl);
        objFileUpload = new FileUpload(List.of(".glb", ".obj"), true, false);
        mainTextureFileUpload = new FileUpload(List.of(".jpg"), true, true);
        otherTexturesFileUpload = new FileUpload(List.of(".jpg"), false, true);
        csvFileUpload = new FileUpload(List.of(".csv"), false, false);

        modelName = new NameTextField("modelUploadForm.modelName.placeholder");

        uploadModelDiv = new UploadLabelContainer(objFileUpload, text("modelUploadForm.uploadModel.label"));
        uploadMainTextureDiv = new UploadLabelContainer(mainTextureFileUpload, text("modelUploadForm.mainTexture.label"));
        uploadOtherTexturesDiv = new UploadLabelContainer(otherTexturesFileUpload, text("modelUploadForm.otherTextures.label"));
        csvOtherTexturesDiv = new UploadLabelContainer(csvFileUpload, text("modelUploadForm.csvTextures.label"));
        uploadOtherTexturesDiv.setEnabled(false);
        csvOtherTexturesDiv.setEnabled(false);

        createButton = new CreateModelButton(this);

        objFileUpload.setUploadListener(
                (fileName, inputStreamMultipartFile) -> {
                    String contentType = fileName.toLowerCase().endsWith(".obj") ? "text/plain" : "model/gltf-binary";
                    modelUrl = createDataUrl(fileName, contentType, inputStreamMultipartFile.getInputStream());
                    modelFileName = fileName;
                    ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "modelId", FileType.MODEL, "main", modelUrl, modelFileName, true, null, true));
                }
        );

        objFileUpload.addFileRemovedListener(event -> {
            ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "modelId", FileType.MODEL, "modelId", true));
        });

        mainTextureFileUpload.setUploadListener(
                (fileName, inputStreamMultipartFile) -> {
                    uploadOtherTexturesDiv.setEnabled(true);
                    csvOtherTexturesDiv.setEnabled(true);
                    textureUrl = createDataUrl(fileName, "image/jpeg", inputStreamMultipartFile.getInputStream());
                    textureName = fileName;
                    ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "modelId", FileType.MAIN, "main", textureUrl, textureName, true, null, true));
                    this.quickTextureEntityMap.put("main",
                            QuickTextureEntity.builder()
                                    .name(fileName)
                                    .csvContent(this.csvMap.getOrDefault(fileName, null))
                                    .textureFileId(fileName)
                                    .build()
                    );
                }
        );

        mainTextureFileUpload.addFileRemovedListener(event -> {
            ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "modelId", FileType.MAIN, "main", true));
            this.quickTextureEntityMap.remove("main");
            uploadOtherTexturesDiv.setEnabled(false);
            csvOtherTexturesDiv.setEnabled(false);
        });

        otherTexturesFileUpload.setUploadListener(
                (fileName, inputStreamMultipartFile) -> {
                    textureUrl = createDataUrl(fileName, "image/jpeg", inputStreamMultipartFile.getInputStream());
                    otherTexturesUrls.add(textureUrl);
                    ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "modelId", FileType.OTHER, fileName, textureUrl, fileName, true, null));
                    this.quickTextureEntityMap.put(fileName,
                            QuickTextureEntity.builder()
                                    .name(fileName)
                                    .textureFileId(fileName)
                                    .csvContent(this.csvMap.getOrDefault(fileName, null))
                                    .build()
                    );
                });

        otherTexturesFileUpload.addFileRemovedListener(event -> {
            ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "modelId", FileType.OTHER, event.getFileName(), true));
            this.quickTextureEntityMap.remove(event.getFileName());
        });

        csvFileUpload.setUploadListener(
                (fileName, uploadedMultipartFile) -> {
                    try (InputStream inputStream = uploadedMultipartFile.getInputStream()) {
                        String base64 = Base64.getEncoder().encodeToString(inputStream.readAllBytes());
                        csvBase64.add(base64);
                        String csvContent = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                        csvUploaded(fileName, csvContent);
                        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "modelId", FileType.CSV, toJpgName(fileName), csvContent, fileName, true, null));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        csvFileUpload.addFileRemovedListener(event -> {
            csvDeleted(event.getFileName());
            ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "modelId", FileType.CSV, toJpgName(event.getFileName()), true));
        });

        modelName.setWidthFull();
        modelName.getStyle().set("min-width", "0");

        vl.setWidthFull();
        vl.setPadding(false);
        vl.add(modelName, uploadModelDiv, uploadMainTextureDiv, uploadOtherTexturesDiv, csvOtherTexturesDiv, createButton);
    }

    private String createDataUrl(String fileName, String contentType, InputStream inputStream) {
        try {
            byte[] fileBytes = inputStream.readAllBytes();
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            return "data:" + contentType + ";base64," + base64Data;
        } catch (IOException e) {
            log.error("Failed to create data URL for file: {}", fileName, e);
            throw new RuntimeException("Failed to create data URL", e);
        }
    }

    private static String toJpgName(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base + ".jpg";
    }

    /**
     * Pre-fills the form with files already stored in the backend (edit mode).
     * Each file is injected into its respective upload component.
     *
     * @param modelFile     the existing model file
     * @param mainTexture   the existing main texture
     * @param otherTextures existing additional textures
     * @param csvFiles      existing CSV files
     */
    public void prefillExistingFiles(InputStreamMultipartFile modelFile,
                                     InputStreamMultipartFile mainTexture,
                                     List<InputStreamMultipartFile> otherTextures,
                                     List<InputStreamMultipartFile> csvFiles) {
        createButton.setUpdateMode();
        if (modelFile != null) {
            objFileUpload.addPrefilledFile(modelFile);
        }
        if (mainTexture != null) {
            mainTextureFileUpload.addPrefilledFile(mainTexture);
            uploadOtherTexturesDiv.setEnabled(true);
        }
        if (otherTextures != null) {
            otherTextures.forEach(otherTexturesFileUpload::addPrefilledFile);
            csvOtherTexturesDiv.setEnabled(true);
        }
        if (csvFiles != null) {
            csvFiles.forEach(csvFileUpload::addPrefilledFile);
        }
    }

    private void csvUploaded(String name, String csvContent) {
        String key = toJpgName(name);
        this.csvMap.put(key, csvContent);
        if (this.quickTextureEntityMap.containsKey(key)) {
            this.quickTextureEntityMap.get(key).setCsvContent(csvContent);
        }
    }

    private void csvDeleted(String name) {
        String key = toJpgName(name);
        this.csvMap.remove(key);
        if (this.quickTextureEntityMap.containsKey(key)) {
            this.quickTextureEntityMap.get(key).setCsvContent(null);
        }
    }
}
