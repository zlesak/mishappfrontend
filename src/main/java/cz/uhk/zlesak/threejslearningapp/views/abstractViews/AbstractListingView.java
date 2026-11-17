package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.NoItemInfoComponent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PageHeader;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PaginationComponent;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ErrorDialog;
import cz.uhk.zlesak.threejslearningapp.components.inputs.ListingToolbar;
import cz.uhk.zlesak.threejslearningapp.components.listItems.EntityRow;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
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
import java.util.function.LongConsumer;
import java.util.function.Supplier;

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
    protected final VerticalLayout listingLayout, paginationLayout, secondaryFilterLayout;
    protected final VerticalLayout filterContentLayout;
    /** The list itself: one {@code <li>} per entity. */
    protected final UnorderedList entityList;
    /** Announcement area for the empty state and for failures, read out when its contents change. */
    protected final Div listMessages;
    protected final PageHeader pageHeader;
    protected final Button filterToggleButton;
    protected final ListingToolbar filter = new ListingToolbar();
    protected final boolean listView;
    protected boolean administrationView;
    @Setter
    private Consumer<Q> entitySelectedListener;
    /** Notified with the number of matching entities whenever the list is rendered. */
    @Setter
    private LongConsumer totalListener;
    /**
     * Builds the way out of an empty listing, shown under the "nothing found" message.
     *
     * <p>On a fresh installation the model picker was simply empty: no explanation and no route
     * onwards, so the user had to work out for themselves that a model has to be uploaded in a
     * different section first. Half the e2e scenarios failed at exactly this point until a fixture
     * created one in advance.
     */
    @Setter
    private Supplier<Component> emptyStateAction;
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
        this.entityList = new UnorderedList();
        this.listMessages = new Div();
        this.paginationLayout = new VerticalLayout();
        this.secondaryFilterLayout = new VerticalLayout();
        this.filterContentLayout = new VerticalLayout(filter);
        this.service = service;

        // The heading names the screen. Inside a picker dialog the dialog already does that, and the
        // dialog's own listing has no page title of its own, so it gets no second heading.
        this.pageHeader = new PageHeader(headingFor(pageTitleKey));
        pageHeader.setVisible(!pageHeader.getHeading().getText().isBlank());
        // The pagination bar states the range in full — "Zobrazeno 1–10 z 31" — so printing a second
        // count under the title would say the same thing twice. The live region stays, because the
        // change still has to be announced.
        pageHeader.setMetaVisuallyHidden();

        listingLayout.addClassName("listing-layout");
        paginationLayout.addClassName("listing-pagination");
        secondaryFilterLayout.addClassName("listing-filter-wrap");
        filterContentLayout.addClassName("listing-filter-content");

        filterToggleButton = new Button(VaadinIcon.ANGLE_DOWN.create());
        filterToggleButton.addClassNames(
                LumoUtility.AlignSelf.START,
                LumoUtility.Margin.Bottom.XSMALL
        );
        filterToggleButton.addClickListener(e -> setFiltersExpanded(!filtersExpanded, true));

        filterParameters = new FilterParameters<>(PageRequest.of(0, 10, Sort.Direction.ASC, "Name"), createFilter(""));

        // Rows, not a grid of cards: a full-width row fits the whole name, so nothing has to be cut.
        entityList.addClassName("listing-rows");
        entityList.getStyle()
                .set("list-style", "none")
                .set("margin", "0")
                .set("padding", "0")
                .set("width", "100%");

        listMessages.addClassName("listing-messages");
        listMessages.getElement().setAttribute("role", "status");
        listMessages.getStyle().set("width", "100%");

        // Contained rather than stretched edge to edge: a row of metadata spread across a 2560 px
        // monitor is as hard to read as one squeezed into a card. The measure is generous enough for a
        // long chapter name and its models, and the whole column is centred.
        Div listBody = new Div(listMessages, entityList);
        listBody.addClassName("listing-body");
        listBody.getStyle()
                .set("width", "100%")
                .set("max-width", "min(1100px, 100%)")
                .set("margin", "0 auto")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m) var(--lumo-space-l)");

        Scroller listScroller = new Scroller(listBody, Scroller.ScrollDirection.VERTICAL);
        listScroller.setSizeFull();

        paginationLayout.addClassNames(LumoUtility.Padding.SMALL);
        paginationLayout.getStyle()
                .set("max-width", "min(1100px, 100%)")
                .set("margin", "0 auto")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)");

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
        listingLayout.add(pageHeader, secondaryFilterLayout, listScroller, paginationLayout);

        getContent().setPadding(false);
        getContent().add(listingLayout);
        getContent().setSizeFull();

        // Gives the toggle its label and its aria-expanded straight away. The viewport-driven call
        // that follows arrives only after a client round trip, and until then the button would be a
        // bare "Filtry" that never said whether the filters were open.
        setFiltersExpanded(true, false);
    }

    /**
     * Creates a list item component for the given entity.
     *
     * @param entity the entity to create a list item for
     * @return an EntityRow component representing the entity
     */
    protected abstract EntityRow createListItem(Q entity);

    /**
     * Switches the listing into administration mode, which shows the edit and delete controls.
     *
     * <p>In that mode the listing lives inside the administration centre, which supplies the page's
     * own H1, so this listing's heading steps aside. The meta line stays: it is what announces how
     * many results a filter left behind.
     *
     * @param administrationView whether to show the administration controls
     */
    public void setAdministrationView(boolean administrationView) {
        this.administrationView = administrationView;
        pageHeader.setHeadingVisible(!administrationView);
    }

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
        entityList.removeAll();
        listMessages.removeAll();
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
            listMessages.add(new NoItemInfoComponent(additionalInfo[0]));
        }

        // The real count, even when this particular page came back empty. Reporting zero told the
        // rest of the screen the listing was empty when it was not, which is a different thing.
        long total = pageResult == null ? 0 : pageResult.total();

        if (entities.isEmpty()) {
            listMessages.add(new NoItemInfoComponent("page.info.noItemsFound"));
            if (emptyStateAction != null) {
                Component guidance = emptyStateAction.get();
                if (guidance != null) {
                    listMessages.add(guidance);
                }
            }
            pageHeader.setMeta(text("listing.meta.empty"));
        } else {
            for (Q entity : entities) {
                EntityRow row = createListItem(entity);
                row.setSelectButtonClickListener(e -> {
                    if (entitySelectedListener != null) {
                        entitySelectedListener.accept(entity);
                    }
                });
                ListItem item = new ListItem(row);
                item.getStyle().set("width", "100%");
                entityList.add(item);
            }

            // Announced through the header's live region: a filter that silently swaps the rows leaves a
            // screen reader user with no way of knowing whether anything happened.
            pageHeader.setMeta(text("listing.meta.shown", entities.size(), total));
        }

        notifyTotal(total);

        // Drawn whenever the listing holds anything at all, including on a page that came back
        // empty: deleting the last item of the last page leaves the user past the end, and
        // without the pager there is nothing on screen that leads back to the items that remain.
        if (total <= 0) {
            return;
        }

        PaginationComponent pagination = new PaginationComponent(
                filterParameters.getPageRequest().getPageNumber(),
                filterParameters.getPageRequest().getPageSize(),
                total,
                p -> {
                    filterParameters.setPageNumber(p);
                    listEntities();
                });
        pagination.setOnPageSizeChange(size -> {
            filterParameters.setPageSize(size);
            listEntities();
        });
        paginationLayout.add(pagination);
    }

    private void notifyTotal(long total) {
        if (totalListener != null) {
            totalListener.accept(total);
        }
    }

    private void showListError() {
        listMessages.add(new ErrorDialog(
                VaadinIcon.WARNING,
                text("listing.error.title"),
                text("listing.error.message"),
                text("listing.error.hint")));
    }

    /**
     * Resolves the heading key belonging to a page title key. The title reads "MISH - Kapitoly",
     * which is right for a browser tab and wrong for an H1.
     *
     * @param pageTitleKey the view's page title key, may be blank
     * @return the heading text, or an empty string when the view has no title of its own
     */
    private String headingFor(String pageTitleKey) {
        if (pageTitleKey == null || pageTitleKey.isBlank()) {
            return "";
        }
        String headingKey = pageTitleKey.replace(".title.", ".heading.");
        String heading = text(headingKey);
        return headingKey.equals(heading) ? text(pageTitleKey) : heading;
    }

    /**
     * Show filtered entities based on the search event.
     *
     * @param event the search event containing the search value
     */
    protected void showFilteredEntities(SearchEvent event) {
        F filter = createFilter(event.getValue());
        // The entity-specific filter only knows about the search text; the narrower questions the
        // toolbar can ask apply to every listing, so they are applied here rather than in each
        // createFilter implementation.
        filter.setCreatorName(event.getCreatorName());
        filter.setCreatedFrom(event.getCreatedFrom());
        filter.setCreatedTo(event.getCreatedTo());
        if (filter instanceof ChapterFilter chapterFilter) {
            chapterFilter.setModelName(event.getModelName());
        }
        filterParameters.setFilteredParameters(event, filter);
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
        filterToggleButton.setText(expanded ? text("filter.toggle.hide") : text("filter.toggle.show"));
        // Without it the button says "show" or "hide" but never says which state it is in.
        filterToggleButton.getElement().setAttribute("aria-expanded", String.valueOf(expanded));

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
