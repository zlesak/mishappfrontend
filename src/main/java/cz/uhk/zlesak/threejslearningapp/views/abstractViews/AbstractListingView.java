package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.NoItemInfoComponent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PaginationComponent;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ErrorDialog;
import cz.uhk.zlesak.threejslearningapp.components.inputs.FilterComponent;
import cz.uhk.zlesak.threejslearningapp.components.listItems.AbstractListItem;
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
import java.util.concurrent.atomic.AtomicLong;
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
    private static final int DESKTOP_BREAKPOINT = 1024;
    protected final VerticalLayout listingLayout, itemListLayout, paginationLayout, secondaryFilterLayout;
    protected final VerticalLayout filterContentLayout;
    protected final Button filterToggleButton;
    protected final FilterComponent filter = new FilterComponent();
    protected final boolean listView;
    @Setter
    protected boolean administrationView;
    @Setter
    private Consumer<Q> entitySelectedListener;
    protected FilterParameters<F> filterParameters;
    protected final S service;
    private final AtomicLong listRequestSequence = new AtomicLong(0);
    private boolean filtersExpanded = true;
    private boolean compactFiltersExpanded = true;
    private String filtersStateKey = "";

    /**
     * Constructor for AbstractListingView.
     * Initializes the view with an option to show or hide the filter.
     * @param listView true for list view mode, false for select mode
     * @param pageTitleKey the title key for the page
     * @param service the service used for entity operations
     * @param showFilter indicates whether to show the filter component
     */
    public AbstractListingView(boolean listView, String pageTitleKey, S service, boolean showFilter) {
        this(listView, pageTitleKey, service);
        secondaryFilterLayout.setVisible(showFilter);

    }

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
        this.secondaryFilterLayout = new VerticalLayout();
        this.filterContentLayout = new VerticalLayout(filter);
        this.service = service;

        listingLayout.addClassName("listing-layout");
        itemListLayout.addClassName("listing-grid");
        paginationLayout.addClassName("listing-pagination");
        secondaryFilterLayout.addClassName("listing-filter-wrap");
        filterContentLayout.addClassName("listing-filter-content");

        filterToggleButton = new Button("Filtry", VaadinIcon.ANGLE_DOWN.create());
        filterToggleButton.addClassNames(
                LumoUtility.AlignSelf.START,
                LumoUtility.Margin.Bottom.XSMALL
        );
        filterToggleButton.addClickListener(e -> setFiltersExpanded(!filtersExpanded, true));

        filterParameters = new FilterParameters<>(PageRequest.of(0, 10, Sort.Direction.ASC, "Name"), createFilter(""));

        itemListLayout.addClassNames(
                LumoUtility.Display.GRID,
                LumoUtility.Grid.Column.COLUMNS_1,
                LumoUtility.Grid.Breakpoint.Small.COLUMNS_2,
                LumoUtility.Grid.Breakpoint.Medium.COLUMNS_3,
                LumoUtility.Grid.Breakpoint.Large.COLUMNS_4,
                LumoUtility.Grid.Breakpoint.XLarge.COLUMNS_5,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );
        itemListLayout.getStyle().set("align-items", "stretch");
        itemListLayout.getStyle().set("grid-auto-rows", "1fr");

        Scroller listScroller = new Scroller(itemListLayout, Scroller.ScrollDirection.VERTICAL);
        listScroller.setSizeFull();

        paginationLayout.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.SMALL
        );

        listingLayout.setFlexGrow(1, listScroller);
        listingLayout.setSizeFull();
        listingLayout.setSpacing(false);
        listingLayout.setPadding(false);
        secondaryFilterLayout.setWidthFull();
        secondaryFilterLayout.setPadding(false);
        secondaryFilterLayout.setSpacing(false);
        filterContentLayout.setWidthFull();
        filterContentLayout.setPadding(false);
        filterContentLayout.setSpacing(false);
        secondaryFilterLayout.add(filterToggleButton, filterContentLayout);
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
    public void listEntities(String... additionalInfo) {
        final long requestId = listRequestSequence.incrementAndGet();
        final String[] info = additionalInfo == null ? new String[0] : additionalInfo.clone();
        itemListLayout.removeAll();
        paginationLayout.removeAll();

        runAsync(
                () -> service.readEntities(filterParameters),
                pageResult -> {
                    if (requestId != listRequestSequence.get()) {
                        return;
                    }
                    renderPageResult(pageResult, info);
                },
                error -> {
                    if (requestId != listRequestSequence.get()) {
                        return;
                    }
                    log.error("Error while listing entities: ", error);
                    showListError();
                }
        );
    }

    private void renderPageResult(PageResult<Q> pageResult, String[] additionalInfo) {
        List<Q> entities = pageResult == null || pageResult.elements() == null
                ? List.of()
                : pageResult.elements().stream().toList();

        if (additionalInfo.length > 0) {
            itemListLayout.add(new NoItemInfoComponent(additionalInfo[0]));
        }

        if (additionalInfo.length > 1) {
            itemListLayout.removeClassNames(
                    LumoUtility.Grid.Breakpoint.Small.COLUMNS_2,
                    LumoUtility.Grid.Breakpoint.Medium.COLUMNS_3,
                    LumoUtility.Grid.Breakpoint.Large.COLUMNS_4,
                    LumoUtility.Grid.Breakpoint.XLarge.COLUMNS_5
            );
        }

        if (entities.isEmpty()) {
            itemListLayout.add(new NoItemInfoComponent("page.info.noItemsFound"));
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

        long total = pageResult.total();
        paginationLayout.add(new PaginationComponent(filterParameters.getPageRequest().getPageNumber(), filterParameters.getPageRequest().getPageSize(), total,
                p -> {
                    filterParameters.setPageNumber(p);
                    listEntities();
                }
        ));
    }

    private void showListError() {
        itemListLayout.add(asFullGridWidth(new ErrorDialog(
                VaadinIcon.WARNING,
                "Interní chyba",
                "Neočekávaná interní chyba aplikace.",
                "Pro více informací kontaktujte správce aplikace.")));
    }

    private <T extends Component> T asFullGridWidth(T component) {
        component.getStyle().set("grid-column", "1 / -1");
        component.getStyle().set("width", "100%");
        return component;
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
        registrations.add(ComponentUtil.addListener(filter, SearchEvent.class, this::showFilteredEntities));
    }

    /**
     * Called after navigation to the view.
     *
     * @param event the after navigation event
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        filter.setSearchFieldValue("");
        filtersStateKey = "listing.filters." + event.getLocation().getPath();
        initializeFilterVisibilityFromClient();
        listEntities();
    }

    private void initializeFilterVisibilityFromClient() {
        UI currentUi = UI.getCurrent();
        if (currentUi == null) {
            setFiltersExpanded(true, false);
            return;
        }

        currentUi.getPage()
                .executeJs("return window.innerWidth;")
                .then(Integer.class, width -> {
                    int viewportWidth = width == null ? DESKTOP_BREAKPOINT : width;
                    if (viewportWidth >= DESKTOP_BREAKPOINT) {
                        applyFilterModeForWidth(viewportWidth);
                        return;
                    }
                    currentUi.getPage()
                            .executeJs("const raw = sessionStorage.getItem($0); return raw === null ? '' : raw;", filtersStateKey)
                            .then(String.class, storedValue -> {
                                if (storedValue != null && !storedValue.isBlank()) {
                                    compactFiltersExpanded = Boolean.parseBoolean(storedValue);
                                } else {
                                    compactFiltersExpanded = viewportWidth > 599;
                                }
                                applyFilterModeForWidth(viewportWidth);
                            });
                });

        registrations.add(currentUi.getPage().addBrowserWindowResizeListener(event -> applyFilterModeForWidth(event.getWidth())));
    }

    private void setFiltersExpanded(boolean expanded, boolean persist) {
        filtersExpanded = expanded;
        filterContentLayout.setVisible(expanded);
        filterToggleButton.setIcon(expanded ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
        filterToggleButton.setText(expanded ? "Filtry (skrýt)" : "Filtry (zobrazit)");

        if (!persist || filtersStateKey == null || filtersStateKey.isBlank()) {
            return;
        }
        UI currentUi = UI.getCurrent();
        if (currentUi != null) {
            compactFiltersExpanded = expanded;
            currentUi.getPage().executeJs("sessionStorage.setItem($0, $1);", filtersStateKey, String.valueOf(expanded));
        }
    }

    private void applyFilterModeForWidth(int viewportWidth) {
        boolean desktop = viewportWidth >= DESKTOP_BREAKPOINT;
        filterToggleButton.setVisible(!desktop);
        if (desktop) {
            setFiltersExpanded(true, false);
        } else {
            setFiltersExpanded(compactFiltersExpanded, false);
        }
    }
}
