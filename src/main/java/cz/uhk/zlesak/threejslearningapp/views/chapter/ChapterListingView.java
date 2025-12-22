package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.lists.AbstractListItem;
import cz.uhk.zlesak.threejslearningapp.components.lists.ChapterListItem;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizCreateView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * ChapterListingView Class - Shows the list of available chapters to the user.
 * It fetches chapter data from the backend and displays it using ChapterListItemComponent.
 */
@Slf4j
@Route("chapters")
@Scope("prototype")
@Tag("chapters-listing")
@PermitAll
public class ChapterListingView extends AbstractListingView<ChapterEntity, ChapterFilter> {
    private final ChapterService chapterService;

    /**
     * Constructor for ChapterListingView.
     * It initializes the view with the necessary controllers and internationalization provider.
     *
     * @param chapterService controller for handling chapter-related operations
     */
    @Autowired
    public ChapterListingView(ChapterService chapterService) {
        super(true, "page.title.chapterListView");
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new ChapterFilter(""));
        this.chapterService = chapterService;
    }

    /**
     * No-args constructor for a dialog window for selecting a chapter in quiz create mode
     *
     * @see QuizCreateView
     */
    public ChapterListingView() {
        super();
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new ChapterFilter(""));
        this.chapterService = SpringContextUtils.getBean(ChapterService.class);
    }

    /**
     * Called after navigation to this view.
     * It fetches the list of chapters from the ChapterService and adds them to the vertical layout.
     * If an error occurs during fetching, it logs the error and shows an error notification
     *
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new ChapterFilter(""));
        filter.setSearchFieldValue(filterParameters.getFilter().getSearchText());
        listEntities();
    }

    /**
     * Fetches a page of ChapterEntity based on the provided filter parameters.
     *
     * @param params filter parameters including pagination and filtering criteria
     * @return a PageResult containing the list of ChapterEntity and pagination details
     */
    @Override
    protected PageResult<ChapterEntity> fetchPage(FilterParameters<ChapterFilter> params) {
        return chapterService.readEntities(params);
    }

    /**
     * Creates a ChapterListItem component for the given ChapterEntity. //TODO chapter should be quick from BE
     *
     * @param chapter the ChapterEntity to create a list item for
     * @return a ChapterListItem component representing the chapter
     */
    @Override
    protected AbstractListItem createListItem(ChapterEntity chapter) {
        return new ChapterListItem(chapter, listView);
    }

    /**
     * Creates a ChapterFilter based on the provided search text.
     *
     * @param searchText the text to filter entities by
     * @return a ChapterFilter object
     */
    @Override
    protected ChapterFilter createFilter(String searchText) {
        return new ChapterFilter(searchText);
    }
}
