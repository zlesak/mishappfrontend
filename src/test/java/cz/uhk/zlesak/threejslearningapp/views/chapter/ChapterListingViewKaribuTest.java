package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Location;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PaginationComponent;
import cz.uhk.zlesak.threejslearningapp.components.inputs.ListingToolbar;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        view.afterNavigation(mockNavigationEvent("chapters"));
        flushUi();

        verify(chapterService, timeout(1000).atLeastOnce()).readEntities(any());
    }

    @Test
    void searchEvent_shouldUpdateFilterAndSortParameters() {
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("chapters"));
        flushUi();

        ListingToolbar filterComponent = (ListingToolbar) ReflectionTestUtils.getField(view, "filter");
        ComponentUtil.fireEvent(filterComponent, new SearchEvent("lebka", Sort.Direction.DESC, "Created", filterComponent));
        flushUi();

        ArgumentCaptor<FilterParameters<ChapterFilter>> captor = filterParametersCaptor();
        verify(chapterService, atLeast(2)).readEntities(captor.capture());

        FilterParameters<?> lastFilter = captor.getAllValues().getLast();
        ChapterFilter chapterFilter = (ChapterFilter) lastFilter.getFilter();

        assertEquals("lebka", chapterFilter.getSearchText());
        assertEquals(Sort.Direction.DESC, lastFilter.getPageRequest().getSort().iterator().next().getDirection());
        assertEquals("Created", lastFilter.getPageRequest().getSort().iterator().next().getProperty());
    }

    @Test
    void theListingHasOneHeadingAndAnnouncesHowManyResultsThereAre() {
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("chapters"));
        MockVaadin.clientRoundtrip(false);

        List<H1> headings = findAll(view, H1.class).stream().filter(Component::isVisible).toList();
        assertEquals(1, headings.size());
        assertEquals("Kapitoly", headings.getFirst().getText());

        // Announced through a live region: a filter that silently swaps the rows leaves someone
        // listening with no idea whether anything happened.
        Span meta = findAll(view, Span.class).stream()
                .filter(span -> "status".equals(span.getElement().getAttribute("role")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The listing has no live region"));
        assertEquals("Zobrazeno 1 z 12", meta.getText());
    }

    @Test
    void theEntriesAreAListAndNotAGridOfCards() {
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("chapters"));
        MockVaadin.clientRoundtrip(false);

        UnorderedList list = findAll(view, UnorderedList.class).getFirst();

        assertEquals(1, list.getChildren().count());
        assertEquals("li", list.getChildren().findFirst().orElseThrow().getElement().getTag());
    }

    @Test
    void anEmptyResultSaysSoInBothPlaces() {
        when(chapterService.readEntities(any())).thenReturn(new PageResult<>(List.of(), 0L, 0));
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("chapters"));
        MockVaadin.clientRoundtrip(false);

        Span meta = findAll(view, Span.class).stream()
                .filter(span -> "status".equals(span.getElement().getAttribute("role")))
                .findFirst()
                .orElseThrow();
        assertEquals("Nenalezena žádná položka", meta.getText());

        assertTrue(findAll(view, H2.class).stream()
                .anyMatch(heading -> "Nebyly nalezeny žádné položky.".equals(heading.getText())));
    }

    @Test
    void aPageThatCameBackEmptyStillOffersAWayBackToTheItemsThatRemain() {
        // Deleting the last chapter on the last page leaves the listing asking for a page past the
        // end: no rows, but eleven chapters still there. Without the pager the screen says nothing
        // was found and offers nothing to click.
        when(chapterService.readEntities(any())).thenReturn(new PageResult<>(List.of(), 11L, 2));
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("chapters"));
        MockVaadin.clientRoundtrip(false);

        assertFalse(findAll(view, PaginationComponent.class).isEmpty(),
                "An empty page of a non-empty listing is a dead end without the pager");
    }

    @Test
    void aTrulyEmptyListingHasNothingToPageThrough() {
        when(chapterService.readEntities(any())).thenReturn(new PageResult<>(List.of(), 0L, 0));
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("chapters"));
        MockVaadin.clientRoundtrip(false);

        assertTrue(findAll(view, PaginationComponent.class).isEmpty());
    }

    @Test
    void theFilterToggleSaysWhetherItIsOpen() {
        ChapterListingView view = new ChapterListingView(chapterService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("chapters"));
        MockVaadin.clientRoundtrip(false);

        Button toggle = (Button) ReflectionTestUtils.getField(view, "filterToggleButton");
        assertNotNull(toggle);
        String expanded = toggle.getElement().getAttribute("aria-expanded");
        assertNotNull(expanded, "The toggle never says which state it is in");

        toggle.click();
        assertEquals(String.valueOf(!Boolean.parseBoolean(expanded)), toggle.getElement().getAttribute("aria-expanded"));
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

    private AfterNavigationEvent mockNavigationEvent(String path) {
        AfterNavigationEvent event = mock(AfterNavigationEvent.class);
        Location location = mock(Location.class);
        when(location.getPath()).thenReturn(path);
        when(event.getLocation()).thenReturn(location);
        return event;
    }
}
