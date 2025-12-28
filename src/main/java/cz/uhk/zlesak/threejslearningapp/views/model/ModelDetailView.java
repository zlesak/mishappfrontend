package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractModelView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

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
    private QuickModelEntity quickModelEntity;

    /**
     * Constructor for ModelDetailView.
     */
    public ModelDetailView() {
        super("page.title.modelView");
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
        loadSingleModelWithTextures(quickModelEntity);

        if (quickModelEntity.getMainTexture() != null) {
            modelDiv.modelTextureAreaSelectContainer.initializeData(Map.of(quickModelEntity.getModel().getId(), quickModelEntity));
        }

        modelUploadForm.listingMode();
    }
}
