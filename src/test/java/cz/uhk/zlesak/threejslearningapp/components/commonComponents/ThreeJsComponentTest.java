package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.quiz.TextureClickedEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.*;
import cz.uhk.zlesak.threejslearningapp.security.AccessTokenProvider;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"SameParameterValue", "unused"})
class ThreeJsComponentTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void clientCallableMethodsShouldEmitEventsAndToken() {
        ThreeJsComponent component = attach(new ThreeJsComponent());

        AtomicReference<TextureClickedEvent> clicked = new AtomicReference<>();
        AtomicReference<ThreeJsDoingActions> doing = new AtomicReference<>();
        AtomicReference<ThreeJsFinishedActions> finished = new AtomicReference<>();
        AtomicReference<ThreeJsLoadingProgress> progress = new AtomicReference<>();
        ComponentUtil.addListener(component, TextureClickedEvent.class, clicked::set);
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsDoingActions.class, doing::set);
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsFinishedActions.class, finished::set);
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsLoadingProgress.class, progress::set);

        component.onColorPicked("model-1", "texture-1", "#AA11CC", "");
        assertNotNull(clicked.get());
        assertEquals("model-1", clicked.get().getModelId());

        component.doingActions("loading");
        assertEquals("loading", doing.get().getDescription());

        component.finishedActions();
        assertNotNull(finished.get());

        component.loadingProgress(42, "textures");
        assertEquals(42, progress.get().getPercent());
        assertEquals("textures", progress.get().getDescription());

        AccessTokenProvider provider = mock(AccessTokenProvider.class);
        when(provider.getValidAccessToken()).thenReturn("token-xyz");
        registerBean(provider);
        assertEquals("token-xyz", component.getToken());
    }

    @Test
    void jsCallbacksShouldInvokeThumbnailAndDisposeCallbacks() throws Exception {
        ThreeJsComponent component = attach(new ThreeJsComponent());
        AtomicReference<String> thumbnail = new AtomicReference<>();
        AtomicReference<Boolean> disposed = new AtomicReference<>(false);

        component.getThumbnailDataUrl("model-1", 64, 64, thumbnail::set);
        String requestId = getOnlyRequestId(component, "thumbnailCallbacks");
        invokeClientCallable(component, "onThumbnailReady", new Class[]{String.class, String.class}, requestId, "data:image/png;base64,abc");
        assertEquals("data:image/png;base64,abc", thumbnail.get());
        invokeClientCallable(component, "onThumbnailReady", new Class[]{String.class, String.class}, requestId, null);
        assertTrue(getCallbackMap(component, "thumbnailCallbacks").isEmpty());

        component.dispose(() -> disposed.set(true));
        invokeClientCallable(component, "notifyDisposed", new Class[0]);
        assertTrue(disposed.get());
    }

    @Test
    void overlappingJsCallbacksShouldResolveByRequestId() throws Exception {
        ThreeJsComponent component = attach(new ThreeJsComponent());
        AtomicReference<String> firstThumbnail = new AtomicReference<>();
        AtomicReference<String> secondThumbnail = new AtomicReference<>();
        AtomicReference<String> firstBackground = new AtomicReference<>();
        AtomicReference<String> secondBackground = new AtomicReference<>();

        component.getThumbnailDataUrl("model-a", 64, 64, firstThumbnail::set);
        String thumbRequestA = getLatestRequestId(component, "thumbnailCallbacks");
        component.getThumbnailDataUrl("model-b", 64, 64, secondThumbnail::set);
        String thumbRequestB = getLatestRequestId(component, "thumbnailCallbacks");

        component.getBackgroundSpecData(firstBackground::set);
        String backgroundRequestA = getLatestRequestId(component, "backgroundSpecCallbacks");
        component.getBackgroundSpecData(secondBackground::set);
        String backgroundRequestB = getLatestRequestId(component, "backgroundSpecCallbacks");

        invokeClientCallable(component, "onThumbnailReady", new Class[]{String.class, String.class}, thumbRequestB, "thumb-B");
        invokeClientCallable(component, "onThumbnailReady", new Class[]{String.class, String.class}, thumbRequestA, "thumb-A");
        invokeClientCallable(component, "onBackgroundSpecReady", new Class[]{String.class, String.class}, backgroundRequestB, "{\"type\":\"b\"}");
        invokeClientCallable(component, "onBackgroundSpecReady", new Class[]{String.class, String.class}, backgroundRequestA, "{\"type\":\"a\"}");

        assertEquals("thumb-A", firstThumbnail.get());
        assertEquals("thumb-B", secondThumbnail.get());
        assertEquals("{\"type\":\"a\"}", firstBackground.get());
        assertEquals("{\"type\":\"b\"}", secondBackground.get());
        assertTrue(getCallbackMap(component, "thumbnailCallbacks").isEmpty());
        assertTrue(getCallbackMap(component, "backgroundSpecCallbacks").isEmpty());
    }

    @Test
    void attachListenersShouldHandleUploadRemoveAndActionEvents() {
        ThreeJsComponent component = attach(new ThreeJsComponent());

        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "model-1", FileType.MODEL, "model-1", "base64", "model.glb", true, "q-1", true));
        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "model-1", FileType.OTHER, "texture-1", "base64", "texture.png", true, "q-1", true));
        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "model-1", FileType.MAIN, "main-1", "base64", "main.png", true, "q-1", true));
        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), "model-1", FileType.CSV, "csv-1", "a;b", "main.csv", true, "q-1", true));

        ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "model-1", FileType.MODEL, "model-1", true));
        ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "model-1", FileType.OTHER, "texture-1", true));
        ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "model-1", FileType.MAIN, "main-1", true));
        ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), "model-1", FileType.CSV, "csv-1", true));

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), "model-1", "texture-1", ThreeJsActions.SWITCH_OTHER_TEXTURE, true, "q-1"));
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), "model-1", null, ThreeJsActions.SHOW_MODEL, true, "q-1"));
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), "model-1", "texture-1", ThreeJsActions.APPLY_MASK_TO_TEXTURE, true, "q-1", "#112233"));
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), "model-1", null, ThreeJsActions.REMOVE, true, "q-1"));

        assertEquals(3, getRegistrations(component).size());
    }

    @Test
    void onDetachShouldClearRegistrationsAndExecutor() throws Exception {
        ThreeJsComponent component = attach(new ThreeJsComponent());

        invokeOnDetach(component, new com.vaadin.flow.component.DetachEvent(component));

        assertTrue(getRegistrations(component).isEmpty());
        assertNull(getJsDispatchExecutor(component));
    }

    @Test
    void loadingAndActionCallbacksShouldThrottleDuplicateEvents() {
        ThreeJsComponent component = attach(new ThreeJsComponent());
        AtomicReference<Integer> progressCount = new AtomicReference<>(0);
        AtomicReference<Integer> actionCount = new AtomicReference<>(0);
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsLoadingProgress.class, e -> progressCount.set(progressCount.get() + 1));
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsDoingActions.class, e -> actionCount.set(actionCount.get() + 1));

        component.loadingProgress(10, "load");
        component.loadingProgress(10, "load");
        component.loadingProgress(100, "done");
        component.doingActions("spin");
        component.doingActions("spin");

        assertEquals(2, progressCount.get());
        assertEquals(1, actionCount.get());
    }

    private ThreeJsComponent attach(ThreeJsComponent component) {
        UI.getCurrent().add(component);
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return component;
    }

    private List<?> getRegistrations(ThreeJsComponent component) {
        try {
            var field = ThreeJsComponent.class.getDeclaredField("registrations");
            field.setAccessible(true);
            return (List<?>) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getCallbackMap(ThreeJsComponent component, String fieldName) {
        try {
            var field = ThreeJsComponent.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Map<String, ?>) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getOnlyRequestId(ThreeJsComponent component, String fieldName) {
        Map<String, ?> map = getCallbackMap(component, fieldName);
        assertEquals(1, map.size());
        return map.keySet().iterator().next();
    }

    private String getLatestRequestId(ThreeJsComponent component, String fieldName) {
        Map<String, ?> map = getCallbackMap(component, fieldName);
        assertFalse(map.isEmpty());
        return map.keySet().stream()
                .mapToLong(Long::parseLong)
                .max()
                .stream()
                .mapToObj(String::valueOf)
                .findFirst()
                .orElseThrow();
    }

    private void invokeClientCallable(ThreeJsComponent component, String methodName, Class<?>[] types, Object... args) throws Exception {
        Method method = ThreeJsComponent.class.getDeclaredMethod(methodName, types);
        method.setAccessible(true);
        method.invoke(component, args);
    }

    private void invokeOnDetach(ThreeJsComponent component, com.vaadin.flow.component.DetachEvent arg) throws Exception {
        Method method = ThreeJsComponent.class.getDeclaredMethod("onDetach", com.vaadin.flow.component.DetachEvent.class);
        method.setAccessible(true);
        method.invoke(component, arg);
    }

    private Object getJsDispatchExecutor(ThreeJsComponent component) throws Exception {
        var field = ThreeJsComponent.class.getDeclaredField("jsDispatchExecutor");
        field.setAccessible(true);
        return field.get(component);
    }

    private void registerBean(AccessTokenProvider bean) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(AccessTokenProvider.class, () -> bean);
        context.refresh();
        SpringContextUtils.setContext(context);
    }

    // --- Additional coverage tests ---

    @Test
    void onThumbnailReadyWithNullOrBlankRequestIdShouldReturnEarlyWithoutCallback() throws Exception {
        // Line 440: early return when requestId is null or blank.
        ThreeJsComponent component = attach(new ThreeJsComponent());
        AtomicReference<String> thumb = new AtomicReference<>("not-called");

        component.getThumbnailDataUrl("model-x", 64, 64, thumb::set);

        invokeClientCallable(component, "onThumbnailReady",
                new Class[]{String.class, String.class}, null, "url");
        invokeClientCallable(component, "onThumbnailReady",
                new Class[]{String.class, String.class}, "", "url");

        // Callback must NOT have been called for null/blank requestId.
        assertEquals("not-called", thumb.get());
        // Real requestId still pending.
        assertFalse(getCallbackMap(component, "thumbnailCallbacks").isEmpty());
    }

    @Test
    void onBackgroundSpecReadyWithNullOrBlankRequestIdShouldReturnEarly() throws Exception {
        // Line 495: early return when requestId is null or blank.
        ThreeJsComponent component = attach(new ThreeJsComponent());
        AtomicReference<String> spec = new AtomicReference<>("not-called");

        component.getBackgroundSpecData(spec::set);

        invokeClientCallable(component, "onBackgroundSpecReady",
                new Class[]{String.class, String.class}, null, "{}");
        invokeClientCallable(component, "onBackgroundSpecReady",
                new Class[]{String.class, String.class}, "", "{}");

        assertEquals("not-called", spec.get());
        assertFalse(getCallbackMap(component, "backgroundSpecCallbacks").isEmpty());
    }

    @Test
    void onColorPickedWithNonBlankQuestionIdShouldFireOnUi() {
        // Line 518: else branch (questionId non-blank → fireEvent on UI).
        ThreeJsComponent component = attach(new ThreeJsComponent());
        AtomicReference<TextureClickedEvent> captured = new AtomicReference<>();
        ComponentUtil.addListener(UI.getCurrent(), TextureClickedEvent.class, captured::set);

        component.onColorPicked("model-1", "texture-1", "#AABBCC", "q-123");

        assertNotNull(captured.get());
        assertEquals("q-123", captured.get().getQuestionId());
        assertEquals("#AABBCC", captured.get().getHexColor());
    }
}
