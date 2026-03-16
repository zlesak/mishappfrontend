package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ChapterListingViewKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ChapterService chapterService;

    @MockitoBean
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        when(chapterService.readEntities(any())).thenReturn(new PageResult<>(List.of(chapter()), 12L, 0));
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void navigation_shouldRenderChapterCardsAndPagination() {
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(null);
        flushUi();

        verify(chapterService, timeout(1000).atLeastOnce()).readEntities(any());
    }

    @Test
    void searchEvent_shouldUpdateFilterAndSortParameters() {
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(null);
        flushUi();

        com.vaadin.flow.component.ComponentUtil.fireEvent(UI.getCurrent(), new SearchEvent("lebka", Sort.Direction.DESC, "Created", UI.getCurrent()));
        flushUi();

        ArgumentCaptor<FilterParameters<ChapterFilter>> captor = filterParametersCaptor();
        verify(chapterService, atLeast(2)).readEntities(captor.capture());

        FilterParameters<?> lastFilter = captor.getAllValues().getLast();
        ChapterFilter chapterFilter = (ChapterFilter) lastFilter.getFilter();

        assertEquals("lebka", chapterFilter.getSearchText());
        assertEquals(Sort.Direction.DESC, lastFilter.getPageRequest().getSort().iterator().next().getDirection());
        assertEquals("Created", lastFilter.getPageRequest().getSort().iterator().next().getProperty());
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }

    private ChapterEntity chapter() {
        return ChapterEntity.builder()
                .id("chapter-1")
                .name("Anatomie")
                .creatorId("teacher")
                .build();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<FilterParameters<ChapterFilter>> filterParametersCaptor() {
        return (ArgumentCaptor<FilterParameters<ChapterFilter>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(FilterParameters.class);
    }
}
