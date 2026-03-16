package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.components.forms.CreateChapterForm;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.NameTextField;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.vaadin.flow.component.ComponentUtil.fireEvent;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ChapterViewsKaribuTest {
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
    void detailViewWithoutChapterIdShouldForwardToListing() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);

        when(event.getRouteParameters()).thenReturn(new RouteParameters());

        view.beforeEnter(event);

        verify(event).forwardTo(ChapterListingView.class);
    }

    @Test
    void detailViewShouldLoadReadOnlyChapterData() throws Exception {
        when(chapterService.getChapterName("chapter-1")).thenReturn("Lebka");
        when(chapterService.getChapterContent("chapter-1")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-1")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-1")).thenReturn(Map.of());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-1"));
        view.afterNavigation(null);
        flushUi();

        NameTextField name = findAll(view, NameTextField.class).getFirst();

        assertTrue(name.isReadOnly());
        verify(chapterService).getChapterName("chapter-1");
        verify(chapterService).getChapterContent("chapter-1");
        verify(chapterService).processHeaders("chapter-1");
    }

    @Test
    void createViewShouldHideSearchAndSwitchButtonToEditMode() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertTrue(findAll(view, SearchTextField.class).isEmpty());

        view.beforeEnter(beforeEnterEvent("chapter-1"));

        CreateChapterForm form = findAll(view, CreateChapterForm.class).getFirst();
        assertEquals("Upravit kapitolu", form.getCreateChapterButton().getText());
    }

    @Test
    void createViewShouldPreferChapterFromSessionBeforeCallingService() throws Exception {
        when(chapterService.getChapterContent("chapter-1")).thenReturn("{\"blocks\":[]}");
        when(chapterService.getChaptersModels("chapter-1")).thenReturn(Map.of());

        ChapterEntity chapter = ChapterEntity.builder()
                .id("chapter-1")
                .name("Lebka")
                .build();
        VaadinSession.getCurrent().setAttribute("chapterEntity", chapter);

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-1"));
        view.afterNavigation(null);
        flushUi();

        assertTrue(findAll(view, SearchTextField.class).isEmpty());
        assertEquals("Upravit kapitolu", findAll(view, CreateChapterForm.class).getFirst().getCreateChapterButton().getText());
        assertNull(VaadinSession.getCurrent().getAttribute("chapterEntity"));
        verify(chapterService).getChapterContent("chapter-1");
        verify(chapterService).getChaptersModels("chapter-1");
        verify(chapterService, org.mockito.Mockito.never()).read(ArgumentMatchers.anyString());
    }

    @Test
    void createViewShouldWrapSaveFailuresFromCreateChapterAndNavigate() {
        when(chapterService.saveChapter(eq(null), eq(false), eq("Nova kapitola"), eq("{\"blocks\":[]}"), any(), eq(null)))
                .thenThrow(new RuntimeException("save failed"));

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        findAll(view, NameTextField.class).getFirst().setValue("Nova kapitola");

        assertDoesNotThrow(() ->
                invoke(view, "createChapterAndNavigate", new Class[]{String.class, Map.class}, "{\"blocks\":[]}", Map.of())
        );
        flushUi();
        verify(chapterService).saveChapter(eq(null), eq(false), eq("Nova kapitola"), eq("{\"blocks\":[]}"), any(), eq(null));
    }

    @Test
    void createViewShouldSaveAndNavigateWhenCreateChapterAndNavigateInvoked() {
        when(chapterService.saveChapter(eq(null), eq(false), eq("Nova kapitola"), eq("{\"blocks\":[]}"), any(), eq(null)))
                .thenReturn("chapter-2");

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        findAll(view, NameTextField.class).getFirst().setValue("Nova kapitola");

        assertDoesNotThrow(() ->
                invoke(view, "createChapterAndNavigate", new Class[]{String.class, Map.class}, "{\"blocks\":[]}", Map.of())
        );
        flushUi();

        verify(chapterService).saveChapter(eq(null), eq(false), eq("Nova kapitola"), eq("{\"blocks\":[]}"), any(), eq(null));
    }

    @Test
    void createViewShouldNavigateAwayWhenLoadChapterFails() {
        when(chapterService.read("chapter-1")).thenThrow(new RuntimeException("load failed"));

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-1"));

        assertDoesNotThrow(() -> view.afterNavigation(null));
        flushUi();
        verify(chapterService).read("chapter-1");
    }

    @Test
    void createViewShouldLoadSelectedModelFromDialogIntoScroller() throws Exception {
        QuickModelEntity selectedModel = QuickModelEntity.builder()
                .metadataId("meta-1")
                .model(ModelFileEntity.builder().id("file-1").name("Quick").related(List.of()).build())
                .build();
        ModelEntity fullModel = ModelEntity.builder()
                .metadataId("meta-1")
                .model(ModelFileEntity.builder().id("file-1").name("Full").related(List.of()).build())
                .otherTextures(List.of())
                .build();
        when(modelService.read("meta-1")).thenReturn(fullModel);

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        Object secondaryNavigation = getField(view, "secondaryNavigation");
        Object modelsScroller = getField(secondaryNavigation, "modelsScroller");
        Method initSelects = modelsScroller.getClass().getMethod("initSelects", Map.class);
        initSelects.invoke(modelsScroller, Map.of());

        fireEvent(UI.getCurrent(), new ModelSelectedFromDialogEvent(UI.getCurrent(), false, selectedModel, "main"));
        flushUi();
        verify(modelService, times(1)).read("meta-1");
    }

    private BeforeEnterEvent beforeEnterEvent(String chapterId) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("chapterId", chapterId));
        return event;
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Object getField(Object target, String name) throws Exception {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Method findMethod(Class<?> type, String name, Class<?>[] parameterTypes) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }

}
