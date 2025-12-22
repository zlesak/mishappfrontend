package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.components.buttons.CreateModelButton;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelCreateEvent;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractModelView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

/**
 * View for creating a new 3D model.
 */
@Slf4j
@Route("createModel")
@Tag("create-model")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class ModelCreateView extends AbstractModelView {
    private final ModelService modelService;

    /**
     * Constructor for ModelCreateView.
     *
     * @param modelService the model service for handling model operations
     */
    @Autowired
    public ModelCreateView(ModelService modelService) {
        super("page.title.createModelView", false);
        this.modelService = modelService;

        CreateModelButton createButton = new CreateModelButton(modelUploadForm);
        modelUploadForm.getVl().add(createButton);
    }

    /**
     * Handles the model upload process.
     * Determines if it's an advanced or basic upload and calls the appropriate service method.
     * Shows notifications based on the result and navigates to the model detail view on success.
     */
    private void handleModelUpload() {
        try {
            QuickModelEntity quickModelEntity = uploadModel();
            showSuccessNotification();
            navigateToModelDetailView(quickModelEntity);
        } catch (ApplicationContextException e) {
            showErrorNotification(text("notification.uploadError"), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while uploading model", e);
            showErrorNotification(text("notification.uploadError"), e.getMessage());
        }
    }

    /**
     * Uploads the model based on the form data.
     * Determines if it's an advanced upload (with textures) or basic upload.
     *
     * @return the uploaded model entity
     */
    private QuickModelEntity uploadModel() {
        if (modelUploadForm.getModelName().getValue() == null || modelUploadForm.getModelName().getValue().trim().isEmpty()) {
            throw new ApplicationContextException(text("model.upload.error.emptyName"));
        }
        if (modelUploadForm.getObjFileUpload().getUploadedFiles().isEmpty()) {
            throw new ApplicationContextException(text("model.upload.error.emptyModelFile"));
        }
        if (modelUploadForm.getIsAdvanced().getValue()) {
            if (modelUploadForm.getMainTextureFileUpload().getUploadedFiles().isEmpty()) {
                throw new ApplicationContextException(text("model.upload.error.emptyModelMainTexture"));
            }
            return modelService.create(
                    ModelEntity.builder()
                            .name(modelUploadForm.getModelName().getValue().trim())
                            .inputStreamMultipartFile(modelUploadForm.getObjFileUpload().getUploadedFiles().getFirst())
                            .fullMainTexture(
                                TextureEntity.builder()
                                .textureFile(
                                    modelUploadForm.getMainTextureFileUpload().getUploadedFiles().getFirst()
                                ).build()
                            )
                            .fullOtherTextures(
                                modelUploadForm.getOtherTexturesFileUpload().getUploadedFiles()
                                    .stream()
                                    .map(file -> TextureEntity.builder().textureFile(file).build())
                                    .collect(java.util.stream.Collectors.toList())
                            )
                            .csvFiles(modelUploadForm.getCsvFileUpload().getUploadedFiles())
                            .isAdvanced(true)
                            .build()
            );
        } else {
            return modelService.create(
                    ModelEntity.builder()
                            .name(modelUploadForm.getModelName().getValue().trim())
                            .inputStreamMultipartFile(modelUploadForm.getObjFileUpload().getUploadedFiles().getFirst())
                            .isAdvanced(false)
                            .build()
            );
        }
    }

    /**
     * Navigates to the model detail view for the given model.
     * Uses VaadinSession to temporarily store the model data.
     * TODO: Replace session storage with proper route parameters once backend supports it
     *
     * @param quickModelEntity the model to display
     */
    private void navigateToModelDetailView(QuickModelEntity quickModelEntity) {
        skipBeforeLeaveDialog = true;
        VaadinSession.getCurrent().setAttribute("quickModelEntity", quickModelEntity);
        UI.getCurrent().navigate("model/" + quickModelEntity.getModel().getId());
    }

    /**
     * Registers the model create event listener when the view is attached.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelCreateEvent.class,
                event -> handleModelUpload()
        ));
    }
}
