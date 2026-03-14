package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.*;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelCreateEvent;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractModelView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

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
    private volatile boolean editDataLoaded = false;
    private volatile boolean editDataLoadingStarted = false;

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
    }

    /**
     * Overridden afterNavigation function to load the model data if modelId is present.
     * In edit mode pre-fills the form name, downloads existing files, and loads the model preview.
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (modelId != null && !editDataLoadingStarted) {
            editDataLoadingStarted = true;
            loadEditDataAsync(modelId);
        }
    }

    private void loadEditDataAsync(String editModelId) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            log.error("Cannot load edit data: UI is not available");
            return;
        }

        runAsync(() -> {
                    ModelEntity entity = service.read(editModelId);
                    if (entity == null) {
                        throw new NotFoundException("Model not found: " + editModelId);
                    }
                    ModelService.ModelPrefillData prefillData = null;
                    try {
                        prefillData = service.buildPrefillData(entity);
                    } catch (Exception e) {
                        log.error("Could not download model file for prefill: {}", e.getMessage(), e);
                    }
                    return new EditLoadResult(entity, prefillData);
                }, result -> {
                    modelEntity = result.modelEntity();
                    editDataLoaded = true;
                    modelUploadForm.getModelName().setValue(modelEntity.getName() != null ? modelEntity.getName() : "");
                    if (result.prefillData() != null) {
                        modelUploadForm.prefillExistingFiles(
                                result.prefillData().modelFile(),
                                result.prefillData().mainTexture(),
                                result.prefillData().otherTextures(),
                                result.prefillData().csvFiles()
                        );
                    }
                    loadSingleModelWithTextures(modelEntity, null, null, true);
                    String backgroundSpecJson = service.resolveBackgroundSpecJson(modelEntity);
                    log.info("ModelCreateView(edit): extracted backgroundSpecJson={}", backgroundSpecJson);
                    if (backgroundSpecJson != null && !backgroundSpecJson.isBlank()) {
                        log.info("ModelCreateView(edit): applying background spec to renderer");
                        modelDiv.renderer.setBackgroundSpec(backgroundSpecJson);
                    } else {
                        log.info("ModelCreateView(edit): no background spec found in model description");
                    }
                }, error -> {
                    log.error("Error loading model for editing: {}", editModelId, error);
                    skipBeforeLeaveDialog = true;
                    showErrorNotification(text("notification.loadError"), error);
                });
    }

    /**
     * Uploads (create) or replaces (update) the model.
     * Textures are optional for both GLB and OBJ models.
     */
    private void uploadModel() {
        try {
            if (modelId != null && !editDataLoaded) {
                showErrorNotification(text("notification.uploadError"), "Data modelu se stále načítají. Zkus to prosím za chvíli.");
                return;
            }
            UI ui = UI.getCurrent();
            if (ui == null) {
                throw new IllegalStateException("UI is not available");
            }
            String modelName = modelUploadForm.getModelName().getValue();
            InputStreamMultipartFile modelFile = modelUploadForm.getObjFileUpload().getUploadedFiles().isEmpty()
                    ? null
                    : modelUploadForm.getObjFileUpload().getUploadedFiles().getFirst();
            InputStreamMultipartFile mainTexture = modelUploadForm.getMainTextureFileUpload().getUploadedFiles().isEmpty()
                    ? null
                    : modelUploadForm.getMainTextureFileUpload().getUploadedFiles().getFirst();
            String thumbnailModelId = modelEntity != null
                    && modelEntity.getModel() != null
                    && modelEntity.getModel().getId() != null
                    ? modelEntity.getModel().getId()
                    : "modelId";

            modelDiv.renderer.getBackgroundSpecData(backgroundSpecJson -> {
                    log.info("ModelCreateView(upload): renderer returned backgroundSpecJson={}", backgroundSpecJson);
                    modelDiv.renderer.getThumbnailDataUrl(thumbnailModelId, 320, 320, dataUrl -> {
                        try {
                            String savedEntityId = service.saveFromUpload(
                                    modelEntity != null ? modelEntity.getMetadataId() : null,
                                    modelName,
                                    modelFile,
                                    mainTexture,
                                    modelUploadForm.getOtherTexturesFileUpload().getUploadedFiles(),
                                    modelUploadForm.getCsvFileUpload().getUploadedFiles(),
                                    dataUrl,
                                    backgroundSpecJson
                            );
                            ui.access(() -> {
                                showSuccessNotification();
                                navigateToModelDetailView(savedEntityId);
                            });
                        } catch (Exception e) {
                            log.error("Unexpected error while saving model", e);
                            ui.access(() -> showErrorNotification(text("notification.uploadError"), e));
                        }
                    });
            });
        } catch (Exception e) {
            showErrorNotification(text("notification.uploadError"), e);
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

    private record EditLoadResult(ModelEntity modelEntity, ModelService.ModelPrefillData prefillData) {
    }
}
