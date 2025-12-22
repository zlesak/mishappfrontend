package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs.ModelListDialog;
import cz.uhk.zlesak.threejslearningapp.components.lists.AbstractListItem;
import cz.uhk.zlesak.threejslearningapp.components.lists.ModelListItem;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterCreateView;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * ModelListingView Class - Shows the list of available 3D models to the user.
 * It fetches model data from the backend and displays it using ModelListItem component.
 */
@Route("models")
@Scope("prototype")
@Tag("models-listing")
@PermitAll
public class ModelListingView extends AbstractListingView<QuickModelEntity, ModelFilter> {
    private final ModelService modelService;

    /**
     * Constructor for ModelListingView.
     * Initializes the view with the necessary services using dependency injection.
     * filterParameters is initialized here for ModelListDialog usage.
     *
     * @see ModelListDialog
     */
    @Autowired
    public ModelListingView(ModelService modelService) {
        super(true, "page.title.modelListView");
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new ModelFilter(""));
        this.modelService = modelService;
        filter.getSearchField().setEnabled(false);
    }

    /**
     * No-args constructor for a dialog window for selecting a model in various chapter create use case
     *
     * @see ChapterCreateView
     */
    public ModelListingView() {
        super();
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new ModelFilter(""));
        this.modelService = SpringContextUtils.getBean(ModelService.class);
        filter.getSearchField().setEnabled(false);
    }

    /**
     * Fetches a page of QuickModelEntity based on the provided filter parameters.
     *
     * @param params the filter parameters including pagination and filtering criteria
     * @return a PageResult containing a list of QuickModelEntity objects
     */
    @Override
    protected PageResult<QuickModelEntity> fetchPage(FilterParameters<ModelFilter> params) {
        return modelService.readEntities(filterParameters);
    }

    /**
     * Creates a ModelListItem for the given QuickModelEntity.
     *
     * @param model the QuickModelEntity to create a list item for
     * @return a ModelListItem representing the given model
     */
    @Override
    protected AbstractListItem createListItem(QuickModelEntity model) {
        return new ModelListItem(model, listView);
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

    /**
     * Called after the navigation to this view is complete.
     * This method populates the list of models by calling listComponents.
     *
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new ModelFilter(""));
        filter.setSearchFieldValue(filterParameters.getFilter().getSearchText());
        listEntities();
    }
}
