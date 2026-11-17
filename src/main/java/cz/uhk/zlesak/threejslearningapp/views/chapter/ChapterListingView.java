package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.listItems.EntityRow;
import cz.uhk.zlesak.threejslearningapp.components.listItems.ChapterListItem;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.QuickChapterEntity;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizCreateView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * ChapterListingView Class - Shows the list of available chapters to the user.
 * It fetches chapter data from the backend and displays it using ChapterListItemComponent.
 */
@Slf4j
@Route("chapters")
@Scope("prototype")
@Tag("chapters-listing")
@PermitAll
public class ChapterListingView extends AbstractListingView<QuickChapterEntity, ChapterFilter, ChapterEntity, ChapterService> {

    /**
     * Constructor for ChapterListingView.
     * It initializes the view with the necessary controllers and internationalization provider.
     *
     * @param chapterService controller for handling chapter-related operations
     */
    @Autowired
    public ChapterListingView(ChapterService chapterService) {
        super(true, "page.title.chapterListView", chapterService);
        // Only chapters contain models, so only this listing offers the filter.
        filter.setModelFilterVisible(true);
    }

    /**
     * No-args constructor for a dialog window for selecting a chapter in quiz create mode
     *
     * @see QuizCreateView
     */
    public ChapterListingView() {
        super(SpringContextUtils.getBean(ChapterService.class));
    }

    /**
     * Creates a ChapterListItem component for the given ChapterEntity. //TODO chapter should be quick from BE
     *
     * @param chapter the ChapterEntity to create a list item for
     * @return a ChapterListItem component representing the chapter
     */
    @Override
    protected EntityRow createListItem(QuickChapterEntity chapter) {
        return new ChapterListItem(chapter, listView, administrationView);
    }

    /**
     * Creates a ChapterFilter based on the provided search text.
     *
     * @param searchText the text to filter entities by
     * @return a ChapterFilter object
     */
    @Override
    protected ChapterFilter createFilter(String searchText) {
        return ChapterFilter.builder().SearchText(searchText).build();
    }
}
