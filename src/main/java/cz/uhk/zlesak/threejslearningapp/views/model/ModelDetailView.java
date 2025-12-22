package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.TextureService;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractModelView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.util.Map;

/**
 * ModelDetailView for displaying a 3D model entity detail
 * It is accessible at the route "/model/:modelId?".
 * The model is loaded from the backend using the ModelService.
 */
@Slf4j
@Route("model/:modelId?")
@Tag("view-model")
@Scope("prototype")
@PermitAll
public class ModelDetailView extends AbstractModelView {
    private final ModelService modelService;
    private final TextureService textureService;
    private QuickModelEntity quickModelEntity;

    /**
     * Constructor for ModelDetailView.
     * Initializes the view with necessary controllers and providers.
     *
     * @param modelService   controller for handling model-related operations
     * @param textureService controller for handling texture-related operations
     */
    @Autowired
    public ModelDetailView(ModelService modelService, TextureService textureService) {
        super("page.title.modelView");
        this.modelService = modelService;
        this.textureService = textureService;
    }

    /**
     * Handles actions before entering the view.
     * It checks for the presence of a modelId parameter and attempts to load the corresponding model.
     * If the modelId is missing or invalid, it forwards the user to the ModelListView.
     * When coming from a creation view, the model is expected to be in the session.
     *
     * @param event before navigation event with event details
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        if (parameters.getParameterNames().isEmpty()) {
            event.forwardTo(ModelListingView.class);
        }

        if (parameters.get("modelId").orElse(null) == null) {
            event.forwardTo(ModelListingView.class);
        }

        //TODO remove after BE implementation of geting model by modelEntityId
        if (VaadinSession.getCurrent().getAttribute("quickModelEntity") != null) {
            this.quickModelEntity = (QuickModelEntity) VaadinSession.getCurrent().getAttribute("quickModelEntity");
        } else {
            event.forwardTo(ModelListingView.class);
        }
    }

    /**
     * Handles actions after navigation to the view.
     * It sets the form to listing mode and populates the model name and texture selectors if a model is loaded.
     *
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        try {
            String modelUrl = modelService.getModelFileBeEndpointUrl(quickModelEntity.getModel().getId());
            String textureUrl = null;
            if (quickModelEntity.getMainTexture() != null) {
                modelUploadForm.getIsAdvanced().setValue(true);
                textureUrl = textureService.getTextureFileBeEndpointUrl(quickModelEntity.getMainTexture().getTextureFileId());
            }
            modelUploadForm.getModelName().setValue(quickModelEntity.getModel().getName());
            modelDiv.renderer.loadModel(modelUrl, textureUrl, quickModelEntity.getModel().getId());

            try {
                if (quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()) {
                    Map<String, String> otherTexturesMap = TextureMapHelper.otherTexturesMap(quickModelEntity.getOtherTextures(), textureService);
                    modelDiv.renderer.addOtherTextures(otherTexturesMap, quickModelEntity.getModel().getId());
                    modelDiv.modelTextureAreaSelectContainer.initializeData(Map.of(quickModelEntity.getModel().getId(), quickModelEntity));
                }
                else {
                    modelDiv.renderer.showModel(quickModelEntity.getModel().getId());
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new ApplicationContextException(e.getMessage());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            new ErrorNotification(text("notification.modelLoadFailed") + e.getMessage(), 5000);
            throw e;
        } finally {
            VaadinSession.getCurrent().setAttribute("quickModelEntity", null);
        }
        modelUploadForm.listingMode();
    }
}
