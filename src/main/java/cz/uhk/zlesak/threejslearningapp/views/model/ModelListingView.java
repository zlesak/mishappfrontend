package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs.ModelListDialog;
import cz.uhk.zlesak.threejslearningapp.components.listItems.AbstractListItem;
import cz.uhk.zlesak.threejslearningapp.components.listItems.ModelListItem;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterCreateView;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * ModelListingView Class - Shows the list of available 3D models to the user.
 * It fetches model data from the backend and displays it using ModelListItem component.
 */
@Route("models")
@Scope("prototype")
@Tag("models-listing")
@PermitAll
public class ModelListingView extends AbstractListingView<QuickModelEntity, ModelFilter, ModelEntity, ModelService> {

    /**
     * Constructor for ModelListingView.
     * Initializes the view with the necessary services using dependency injection.
     * filterParameters is initialized here for ModelListDialog usage.
     * @param modelService service for handling model-related operations
     *
     * @see ModelListDialog
     */
    @Autowired
    public ModelListingView(ModelService modelService) {
        super(true, "page.title.modelListView", modelService);
        filter.getSearchField().setEnabled(false);
    }

    /**
     * No-args constructor for a dialog window for selecting a model in various chapter create use case
     *
     * @see ChapterCreateView
     */
    public ModelListingView() {
        super(SpringContextUtils.getBean(ModelService.class));
        filter.getSearchField().setEnabled(false);
    }

    /**
     * Creates a ModelListItem for the given QuickModelEntity.
     *
     * @param model the QuickModelEntity to create a list item for
     * @return a ModelListItem representing the given model
     */
    @Override
    protected AbstractListItem createListItem(QuickModelEntity model) {
        return new ModelListItem(model, listView, administrationView);
    }

    /**
     * Creates a filter object based on the provided search text.
     *
     * @param searchText the text to filter entities by
     * @return a filter object of type F
     */
    @Override
    protected ModelFilter createFilter(String searchText) {
        return new ModelFilter(searchText);
    }
}
