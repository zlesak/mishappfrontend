package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.*;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelCreateEvent;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractModelView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View for creating or updating a 3D model.
 * Extends AbstractModelView and provides functionality to upload and create models.
 */
@Slf4j
@Route("createModel/:modelId?")
@Tag("create-model")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class ModelCreateView extends AbstractModelView {
    private String modelId;
    private ModelEntity modelEntity;

    /**
     * Constructor for ModelCreateView.
     *
     * @param modelService the model service for handling model operations
     */
    @Autowired
    public ModelCreateView(ModelService modelService) {
        super("page.title.createModelView", false, modelService);
    }

    /**
     * Checks for the optional {@code modelId} route parameter.
     * When present the view enters edit mode: the full {@link ModelEntity} is loaded from the
     * backend via {@link ModelService#read}, giving access to all textures and their CSV content.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        modelId = parameters.get("modelId").orElse(null);
        if (modelId != null) {
            try {
                this.modelEntity = service.read(modelId);
                if (this.modelEntity == null) {
                    log.error("Model not found for editing: {}", modelId);
                    skipBeforeLeaveDialog = true;
                    throw new NotFoundException("Model not found: " + modelId);
                }
            } catch (NotFoundException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error loading model for editing: {}", modelId, e);
                skipBeforeLeaveDialog = true;
                throw new NotFoundException("Could not load model: " + modelId);
            }
        }
    }

    /**
     * Overridden afterNavigation function to load the model data if modelId is present.
     * In edit mode pre-fills the form name, downloads existing files, and loads the model preview.
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (modelId != null && modelEntity != null) {
            modelUploadForm.getModelName().setValue(modelEntity.getName() != null ? modelEntity.getName() : "");
            prefillFormFiles();
            loadSingleModelWithTextures(modelEntity, null, null, true);
        }
    }

    /**
     * Downloads the existing model file, main texture, and other textures from the BE and pre-fills the upload form.
     * Download errors are logged but non-fatal – the user can re-upload any file.
     */
    private void prefillFormFiles() {
        try {
            InputStreamMultipartFile modelFile = service.downloadFile(modelEntity.getModel().getId());

            InputStreamMultipartFile mainTexture = null;
            if (modelEntity.getMainTexture() != null) {
                try {
                    mainTexture = service.downloadFile(modelEntity.getMainTexture().getId());
                    mainTexture.setDisplayName(modelEntity.getMainTexture().getName());
                } catch (Exception e) {
                    log.warn("Could not download main texture for prefill: {}", e.getMessage());
                }
            }

            List<InputStreamMultipartFile> otherTextures = new ArrayList<>();
            List<InputStreamMultipartFile> csvFiles = new ArrayList<>();

            if (modelEntity.getOtherTextures() != null) {
                for (QuickTextureEntity tex : modelEntity.getOtherTextures()) {
                    try {
                        InputStreamMultipartFile ot = service.downloadFile(tex.getId());
                        ot.setDisplayName(tex.getName());
                        otherTextures.add(ot);
                        if (tex.getCsvContent() != null && !tex.getCsvContent().isEmpty()) {
                            String csvName = toCsvName(ot.getOriginalFilename());
                            csvFiles.add(new InputStreamMultipartFile(
                                    new java.io.ByteArrayInputStream(tex.getCsvContent().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                    csvName, csvName));
                        }
                    } catch (Exception e) {
                        log.warn("Could not download other texture {} for prefill: {}", tex.getId(), e.getMessage());
                    }
                }
            }

            modelUploadForm.prefillExistingFiles(
                    modelFile,
                    mainTexture,
                    otherTextures.isEmpty() ? null : otherTextures,
                    csvFiles.isEmpty() ? null : csvFiles
            );
        } catch (Exception e) {
            log.error("Could not download model file for prefill: {}", e.getMessage(), e);
        }
    }

    private static String toCsvName(String textureName) {
        if (textureName == null) return "texture.csv";
        int dot = textureName.lastIndexOf('.');
        return (dot > 0 ? textureName.substring(0, dot) : textureName) + ".csv";
    }

    /**
     * Uploads (create) or replaces (update) the model.
     * Textures are optional for both GLB and OBJ models.
     */
    private void uploadModel() {
        try {
            if (modelUploadForm.getModelName().getValue() == null || modelUploadForm.getModelName().getValue().trim().isEmpty()) {
                throw new ApplicationContextException(text("model.upload.error.emptyName"));
            }
            if (modelUploadForm.getObjFileUpload().getUploadedFiles().isEmpty()) {
                throw new ApplicationContextException(text("model.upload.error.emptyModelFile"));
            }

            boolean hasMainTexture = !modelUploadForm.getMainTextureFileUpload().getUploadedFiles().isEmpty();
            boolean hasOtherTextures = !modelUploadForm.getOtherTexturesFileUpload().getUploadedFiles().isEmpty();
            boolean hasTextures = hasMainTexture || hasOtherTextures;

            final ModelEntity.ModelEntityBuilder<?, ?> builder = ModelEntity.builder()
                    .name(modelUploadForm.getModelName().getValue().trim())
                    .inputStreamMultipartFile(modelUploadForm.getObjFileUpload().getUploadedFiles().getFirst())
                    .isAdvanced(hasTextures);

            if (hasMainTexture) {
                builder.fullMainTexture(
                        TextureEntity.builder()
                                .textureFile(modelUploadForm.getMainTextureFileUpload().getUploadedFiles().getFirst())
                                .build()
                );
            }
            if (hasOtherTextures) {
                builder.fullOtherTextures(
                        modelUploadForm.getOtherTexturesFileUpload().getUploadedFiles()
                                .stream()
                                .map(file -> TextureEntity.builder().textureFile(file).build())
                                .collect(Collectors.toList())
                );
            }
            if (!modelUploadForm.getCsvFileUpload().getUploadedFiles().isEmpty()) {
                builder.csvFiles(modelUploadForm.getCsvFileUpload().getUploadedFiles());
            }

            boolean isUpdate = modelId != null;

            modelDiv.renderer.getThumbnailDataUrl("modelId", 256, 256, dataUrl -> {
                try {
                    if (dataUrl != null) {
                        builder.description(dataUrl);
                    }

                    if (isUpdate) {
                        String updatedEntityId = service.update(modelEntity.getMetadataId(), builder.build());
                        UI.getCurrent().access(() -> {
                            showSuccessNotification();
                            navigateToModelDetailView(updatedEntityId);
                        });
                    } else {
                        String createdEntityId = service.create(builder.build());
                        UI.getCurrent().access(() -> {
                            showSuccessNotification();
                            navigateToModelDetailView(createdEntityId);
                        });
                    }
                } catch (ApplicationContextException e) {
                    UI.getCurrent().access(() -> showErrorNotification(text("notification.uploadError"), e.getMessage()));
                } catch (Exception e) {
                    log.error("Unexpected error while saving model", e);
                    UI.getCurrent().access(() -> showErrorNotification(text("notification.uploadError"), e.getMessage()));
                }
            });
        } catch (ApplicationContextException e) {
            showErrorNotification(text("notification.uploadError"), e.getMessage());
        }
    }

    private void navigateToModelDetailView(String id) {
        skipBeforeLeaveDialog = true;
        UI.getCurrent().navigate(ModelDetailView.class, new RouteParameters(new RouteParam("modelId", id)));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelCreateEvent.class,
                event -> uploadModel()
        ));
    }
}
