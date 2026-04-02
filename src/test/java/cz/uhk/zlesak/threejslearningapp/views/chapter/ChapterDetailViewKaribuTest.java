package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ChapterDetailViewKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ChapterService chapterService;

    @MockitoBean
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void shouldForwardToListingWhenChapterIdMissing() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters());
        view.beforeEnter(event);
        verify(event).forwardTo(ChapterListingView.class);
    }

    @Test
    void shouldNotForwardWhenChapterIdPresent() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("chapterId", "ch-1"));
        view.beforeEnter(event);
        verify(event, never()).forwardTo(any(Class.class));
    }

    @Test
    void afterNavigation_shouldLoadChapterDataSuccessfully() throws Exception {
        when(chapterService.getChapterName("ch-1")).thenReturn("Test Chapter");
        when(chapterService.getChapterContent("ch-1")).thenReturn("{}");
        when(chapterService.processHeaders("ch-1")).thenReturn(Collections.emptyMap());
        when(chapterService.getChaptersModels("ch-1")).thenReturn(Collections.emptyMap());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        BeforeEnterEvent enterEvent = mock(BeforeEnterEvent.class);
        when(enterEvent.getRouteParameters()).thenReturn(new RouteParameters("chapterId", "ch-1"));
        view.beforeEnter(enterEvent);

        UI.getCurrent().add(view);
        AfterNavigationEvent event = mock(AfterNavigationEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("chapterId", "ch-1"));
        assertDoesNotThrow(() -> view.afterNavigation(event));
    }

    @Test
    void afterNavigation_shouldHandleErrorGracefully() throws Exception {
        when(chapterService.getChapterName(any())).thenThrow(new RuntimeException("Network error"));

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        BeforeEnterEvent enterEvent = mock(BeforeEnterEvent.class);
        when(enterEvent.getRouteParameters()).thenReturn(new RouteParameters("chapterId", "ch-err"));
        view.beforeEnter(enterEvent);

        UI.getCurrent().add(view);
        AfterNavigationEvent event = mock(AfterNavigationEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("chapterId", "ch-err"));
        assertDoesNotThrow(() -> view.afterNavigation(event));
    }
}
