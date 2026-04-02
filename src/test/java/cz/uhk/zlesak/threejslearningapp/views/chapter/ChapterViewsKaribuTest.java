package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.components.containers.ChapterTabSheetContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.SubchapterSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
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
        verify(chapterService, never()).read(anyString());
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

    @Test
    void createViewConstructorShouldRenderNameTextField() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, NameTextField.class).isEmpty());
    }

    @Test
    void createViewConstructorShouldRenderCreateChapterForm() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, CreateChapterForm.class).isEmpty());
    }

    @Test
    void createViewConstructorShouldNotContainSearchTextField() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertTrue(findAll(view, SearchTextField.class).isEmpty());
    }

    @Test
    void createViewButtonLabelIsCreateBeforeAnyNavigation() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertEquals("Vytvořit kapitolu", findAll(view, CreateChapterForm.class).getFirst().getCreateChapterButton().getText());
    }

    @Test
    void createViewBeforeEnterWithoutParamsShouldNotCallServiceRead() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterWithNoParams());
        view.afterNavigation(null);
        flushUi();

        verify(chapterService, never()).read(anyString());
    }

    @Test
    void createViewBeforeEnterWithBlankChapterIdShouldNotCallServiceRead() {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("chapterId", ""));

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(event);
        view.afterNavigation(null);
        flushUi();

        verify(chapterService, never()).read(anyString());
    }

    @Test
    void createViewAfterNavigationWithoutEditModeDoesNotCallServiceRead() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.afterNavigation(null);
        flushUi();

        verify(chapterService, never()).read(anyString());
    }

    @Test
    void createViewAfterNavigationWithChapterIdCallsServiceRead() throws Exception {
        when(chapterService.read("chapter-load")).thenReturn(
                ChapterEntity.builder().id("chapter-load").name("Loaded").build());
        when(chapterService.getChapterContent("chapter-load")).thenReturn("{\"blocks\":[]}");
        when(chapterService.getChaptersModels("chapter-load")).thenReturn(Map.of());

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-load"));
        view.afterNavigation(null);
        flushUi();

        verify(chapterService).read("chapter-load");
    }

    @Test
    void createViewAfterNavigationWithChapterIdSetsNameTextField() throws Exception {
        when(chapterService.read("chapter-name")).thenReturn(
                ChapterEntity.builder().id("chapter-name").name("Chapter Title").build());
        when(chapterService.getChapterContent("chapter-name")).thenReturn("{\"blocks\":[]}");
        when(chapterService.getChaptersModels("chapter-name")).thenReturn(Map.of());

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-name"));
        view.afterNavigation(null);
        flushUi();
        MockVaadin.clientRoundtrip(false);

        assertEquals("Chapter Title", findAll(view, NameTextField.class).getFirst().getValue());
    }

    @Test
    void createViewDoubleAfterNavigationCallsServiceReadOnlyOnce() throws Exception {
        when(chapterService.read("chapter-once")).thenReturn(
                ChapterEntity.builder().id("chapter-once").name("Once").build());
        when(chapterService.getChapterContent("chapter-once")).thenReturn("{\"blocks\":[]}");
        when(chapterService.getChaptersModels("chapter-once")).thenReturn(Map.of());

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-once"));
        view.afterNavigation(null);
        view.afterNavigation(null);
        flushUi();

        verify(chapterService, times(1)).read("chapter-once");
    }

    @Test
    void createViewNameTextFieldIsEditable() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, NameTextField.class).getFirst().isReadOnly());
    }

    @Test
    void createViewShouldContainChapterTabSheetContainer() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, ChapterTabSheetContainer.class).isEmpty());
    }

    @Test
    void createViewSearchTextFieldRemainsAbsentAfterBeforeEnterWithId() {
        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-1"));

        assertTrue(findAll(view, SearchTextField.class).isEmpty());
    }

    @Test
    void createViewModelSelectedEventWithNullMetadataIdDoesNotCallModelService() throws Exception {
        QuickModelEntity model = QuickModelEntity.builder()
                .metadataId(null)
                .model(ModelFileEntity.builder().id("file-1").name("Model").related(List.of()).build())
                .build();

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        Object secondaryNavigation = getField(view, "secondaryNavigation");
        Object modelsScroller = getField(secondaryNavigation, "modelsScroller");
        Method initSelects = modelsScroller.getClass().getMethod("initSelects", Map.class);
        initSelects.invoke(modelsScroller, Map.of());

        fireEvent(UI.getCurrent(), new ModelSelectedFromDialogEvent(UI.getCurrent(), false, model, "main"));
        flushUi();

        verify(modelService, never()).read(anyString());
    }

    @Test
    void createViewModelSelectedEventWithBlankMetadataIdDoesNotCallModelService() throws Exception {
        QuickModelEntity model = QuickModelEntity.builder()
                .metadataId("   ")
                .model(ModelFileEntity.builder().id("file-2").name("Model2").related(List.of()).build())
                .build();

        ChapterCreateView view = new ChapterCreateView(chapterService, modelService);
        UI.getCurrent().add(view);

        Object secondaryNavigation = getField(view, "secondaryNavigation");
        Object modelsScroller = getField(secondaryNavigation, "modelsScroller");
        Method initSelects = modelsScroller.getClass().getMethod("initSelects", Map.class);
        initSelects.invoke(modelsScroller, Map.of());

        fireEvent(UI.getCurrent(), new ModelSelectedFromDialogEvent(UI.getCurrent(), false, model, "main"));
        flushUi();

        verify(modelService, never()).read(anyString());
    }

    @Test
    void detailViewConstructorShouldContainNameTextField() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, NameTextField.class).isEmpty());
    }

    @Test
    void detailViewConstructorShouldContainSubchapterSelectContainer() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, SubchapterSelectContainer.class).isEmpty());
    }

    @Test
    void detailViewConstructorShouldContainSearchTextField() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, SearchTextField.class).isEmpty());
    }

    @Test
    void detailViewConstructorShouldContainEditorJs() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertFalse(findAll(view, EditorJs.class).isEmpty());
    }

    @Test
    void detailViewBeforeEnterWithValidChapterIdShouldNotForwardToListing() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("chapterId", "chapter-valid"));

        view.beforeEnter(event);

        verify(event, never()).forwardTo(ChapterListingView.class);
    }

    @Test
    void detailViewAfterNavigationShouldCallGetChapterName() throws Exception {
        when(chapterService.getChapterName("chapter-det")).thenReturn("Detail");
        when(chapterService.getChapterContent("chapter-det")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-det")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-det")).thenReturn(Map.of());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-det"));
        view.afterNavigation(null);
        flushUi();

        verify(chapterService).getChapterName("chapter-det");
    }

    @Test
    void detailViewAfterNavigationShouldCallGetChapterContent() throws Exception {
        when(chapterService.getChapterName("chapter-det2")).thenReturn("Detail2");
        when(chapterService.getChapterContent("chapter-det2")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-det2")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-det2")).thenReturn(Map.of());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-det2"));
        view.afterNavigation(null);
        flushUi();

        verify(chapterService).getChapterContent("chapter-det2");
    }

    @Test
    void detailViewAfterNavigationShouldCallProcessHeaders() throws Exception {
        when(chapterService.getChapterName("chapter-hdr")).thenReturn("Hdr");
        when(chapterService.getChapterContent("chapter-hdr")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-hdr")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-hdr")).thenReturn(Map.of());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-hdr"));
        view.afterNavigation(null);
        flushUi();

        verify(chapterService).processHeaders("chapter-hdr");
    }

    @Test
    void detailViewAfterNavigationShouldCallGetChaptersModels() throws Exception {
        when(chapterService.getChapterName("chapter-mdl")).thenReturn("Mdl");
        when(chapterService.getChapterContent("chapter-mdl")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-mdl")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-mdl")).thenReturn(Map.of());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-mdl"));
        view.afterNavigation(null);
        flushUi();

        verify(chapterService).getChaptersModels("chapter-mdl");
    }

    @Test
    void detailViewAfterNavigationShouldSetChapterNameInTextField() throws Exception {
        when(chapterService.getChapterName("chapter-title")).thenReturn("Expected Title");
        when(chapterService.getChapterContent("chapter-title")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-title")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-title")).thenReturn(Map.of());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-title"));
        view.afterNavigation(null);
        flushUi();
        MockVaadin.clientRoundtrip(false);

        assertEquals("Expected Title", findAll(view, NameTextField.class).getFirst().getValue());
    }

    @Test
    void detailViewAfterNavigationWithServiceErrorShouldNotThrow() {
        when(chapterService.getChapterName("chapter-err")).thenThrow(new RuntimeException("service down"));

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-err"));

        assertDoesNotThrow(() -> {
            view.afterNavigation(null);
            flushUi();
        });
    }

    @Test
    void detailViewNameTextFieldShouldBeReadOnly() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertTrue(findAll(view, NameTextField.class).getFirst().isReadOnly());
    }

    @Test
    void detailViewShouldNotContainCreateChapterForm() {
        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);

        assertTrue(findAll(view, CreateChapterForm.class).isEmpty());
    }

    @Test
    void detailViewAfterNavigationWithModelsShouldCallModelService() throws Exception {
        QuickModelEntity quickModel = QuickModelEntity.builder()
                .metadataId("meta-det-1")
                .model(ModelFileEntity.builder().id("file-det-1").name("DetModel").related(List.of()).build())
                .build();
        ModelEntity fullModel = ModelEntity.builder()
                .metadataId("meta-det-1")
                .model(ModelFileEntity.builder().id("file-det-1").name("DetModel").related(List.of()).build())
                .otherTextures(List.of())
                .build();

        when(chapterService.getChapterName("chapter-mod")).thenReturn("ModChapter");
        when(chapterService.getChapterContent("chapter-mod")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-mod")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-mod")).thenReturn(Map.of("main", quickModel));
        when(modelService.read("meta-det-1")).thenReturn(fullModel);

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-mod"));
        view.afterNavigation(null);
        flushUi();

        verify(modelService).read("meta-det-1");
    }

    @Test
    void detailViewAfterNavigationWithEmptyModelsMapDoesNotCallModelService() throws Exception {
        when(chapterService.getChapterName("chapter-nmod")).thenReturn("NoMod");
        when(chapterService.getChapterContent("chapter-nmod")).thenReturn("{\"blocks\":[]}");
        when(chapterService.processHeaders("chapter-nmod")).thenReturn(Map.of());
        when(chapterService.getChaptersModels("chapter-nmod")).thenReturn(Map.of());

        ChapterDetailView view = new ChapterDetailView(chapterService, modelService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("chapter-nmod"));
        view.afterNavigation(null);
        flushUi();

        verify(modelService, never()).read(anyString());
    }

    private BeforeEnterEvent beforeEnterWithNoParams() {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters());
        return event;
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }

}
