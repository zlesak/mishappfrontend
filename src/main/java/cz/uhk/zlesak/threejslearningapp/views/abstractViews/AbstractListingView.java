package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.common.Filter;
import cz.uhk.zlesak.threejslearningapp.components.common.NoItemInfo;
import cz.uhk.zlesak.threejslearningapp.components.common.Pagination;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ErrorDialog;
import cz.uhk.zlesak.threejslearningapp.components.lists.AbstractListItem;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import cz.uhk.zlesak.threejslearningapp.services.AbstractService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Consumer;

/**
 * AbstractListingView, abstract view for displaying a list of entities with filtering and pagination capabilities.
 *
 * @param <Q> the type of entity to be listed - quick type
 * @param <F> the type of filter used for listing entities
 * @param <E> the type of entity managed by the service
 * @param <S> the type of service used for entity operations
 */
@Slf4j
@Scope("prototype")
@Tag("listing-scaffold")
public abstract class AbstractListingView<Q extends AbstractEntity, F extends FilterBase, E extends Q, S extends AbstractService<E, Q, F>> extends AbstractView<S> {
    protected final VerticalLayout listingLayout, itemListLayout, paginationLayout, secondaryFilterLayout;
    protected final Filter filter = new Filter();
    protected final boolean listView;
    @Setter
    protected boolean administrationView;
    @Setter
    private Consumer<Q> entitySelectedListener;
    protected FilterParameters<F> filterParameters;
    protected final S service;

    /**
     * Constructor for AbstractListingView.
     * Initializes the view in non-list mode with an empty page title key.
     *
     * @param service the service used for entity operations
     */
    public AbstractListingView(S service) {
        this(false, "", service);
    }

    /**
     * Constructor for AbstractListingView.
     *
     * @param listView     indicates whether the view is in list view mode or select mode (in cases of model or chapter selection dialogs)
     * @param pageTitleKey the title key for the page
     * @param service      the service used for entity operations
     */
    public AbstractListingView(boolean listView, String pageTitleKey, S service) {
        super(pageTitleKey, service);
        this.listView = listView;
        this.listingLayout = new VerticalLayout();
        this.itemListLayout = new VerticalLayout();
        this.paginationLayout = new VerticalLayout();
        this.secondaryFilterLayout = new VerticalLayout(filter);
        this.service = service;

        filterParameters = new FilterParameters<>(PageRequest.of(0, 10, Sort.Direction.ASC, "Name"), createFilter(""));

        itemListLayout.addClassNames(
                LumoUtility.Display.GRID,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );
        itemListLayout.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(350px, 1fr))");

        Scroller listScroller = new Scroller(itemListLayout, Scroller.ScrollDirection.VERTICAL);
        listScroller.setSizeFull();

        paginationLayout.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.MEDIUM
        );

        listingLayout.setFlexGrow(1, listScroller);
        listingLayout.setSizeFull();
        listingLayout.setSpacing(false);
        listingLayout.setPadding(false);
        listingLayout.add(secondaryFilterLayout, listScroller, paginationLayout);

        getContent().setPadding(false);
        getContent().add(listingLayout);
        getContent().setSizeFull();
    }

    /**
     * Creates a list item component for the given entity.
     *
     * @param entity the entity to create a list item for
     * @return an AbstractListItem component representing the entity
     */
    protected abstract AbstractListItem createListItem(Q entity);

    /**
     * Creates a filter object based on the provided search text.
     *
     * @param searchText the text to filter entities by
     * @return a filter object of type F
     */
    protected abstract F createFilter(String searchText);

    /**
     * Lists entities based on the current filter parameters and updates the UI components.
     */
    public void listEntities() {
        itemListLayout.removeAll();
        paginationLayout.removeAll();

        try {
            PageResult<Q> pageResult = service.readEntities(filterParameters);
            List<Q> entities = pageResult.elements().stream().toList();

            if (entities.isEmpty()) {
                itemListLayout.add(new NoItemInfo("page.info.noItemsFound"));
                return;
            }

            for (Q entity : entities) {
                AbstractListItem itemComponent = createListItem(entity);
                itemComponent.setSelectButtonClickListener(e -> {
                    if (entitySelectedListener != null) {
                        entitySelectedListener.accept(entity);
                    }
                });
                itemListLayout.add(itemComponent);
            }
            paginationLayout.add(new Pagination(filterParameters.getPageRequest().getPageNumber(), filterParameters.getPageRequest().getPageSize(), pageResult.total(),
                    p -> {
                        filterParameters.setPageNumber(p);
                        listEntities();
                    }
            ));
        } catch (Exception e) {
            log.error("Error while listing entities: ", e);
            itemListLayout.add(new ErrorDialog(
                    VaadinIcon.WARNING,
                    "Interní chyba",
                    "Neočekávaná interní chyba aplikace.",
                    "Pro více informací kontaktujte správce aplikace."));
        }
    }

    /**
     * Show filtered entities based on the search event.
     *
     * @param event the search event containing the search value
     */
    protected void showFilteredEntities(SearchEvent event) {
        filterParameters.setFilteredParameters(event, createFilter(event.getValue()));
        listEntities();
    }

    /**
     * Called when the component is attached to the UI.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(attachEvent.getUI(), SearchEvent.class, this::showFilteredEntities));
    }

    /**
     * Called after navigation to the view.
     *
     * @param event the after navigation event
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        filter.setSearchFieldValue("");
        listEntities();
    }
}
