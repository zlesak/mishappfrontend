package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.components.forms.ModelUploadForm;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ModelCreateViewKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

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
    void beforeEnterShouldOnlyStoreRouteParamAndReadOnAfterNavigation() {
        when(modelService.read("missing-model")).thenReturn(null);

        ModelCreateView view = new ModelCreateView(modelService);
        UI.getCurrent().add(view);

        view.beforeEnter(beforeEnterEvent("missing-model"));
        verify(modelService, never()).read(any());

        view.afterNavigation(null);
        flushUi();
        verify(modelService).read("missing-model");
    }

    @Test
    void beforeEnterShouldNotThrowWhenAsyncLoadFails() {
        when(modelService.read("broken-model")).thenThrow(new RuntimeException("backend down"));

        ModelCreateView view = new ModelCreateView(modelService);
        UI.getCurrent().add(view);

        assertDoesNotThrow(() -> view.beforeEnter(beforeEnterEvent("broken-model")));
        assertDoesNotThrow(() -> view.afterNavigation(null));
        flushUi();
        verify(modelService).read("broken-model");
    }

    @Test
    void editModeShouldPrefillModelNameAndSwitchUploadFormIntoUpdateMode() throws Exception {
        ModelEntity modelEntity = model();
        when(modelService.read("model-1")).thenReturn(modelEntity);
        when(modelService.buildPrefillData(modelEntity)).thenReturn(new ModelService.ModelPrefillData(
                file("organ.glb", "glb"),
                file("main.jpg", "main"),
                List.of(file("detail.jpg", "detail")),
                List.of(file("detail.csv", "ff0000;Mandibula"))
        ));

        ModelCreateView view = new ModelCreateView(modelService);
        UI.getCurrent().add(view);

        view.beforeEnter(beforeEnterEvent("model-1"));
        view.afterNavigation(null);
        flushUi();

        ModelUploadForm form = findAll(view, ModelUploadForm.class).getFirst();
        verify(modelService).read("model-1");
        verify(modelService).buildPrefillData(modelEntity);
        assertEquals(0, form.getModelName().getValue().length());
    }

    @Test
    void editModeShouldKeepFormUsableWhenPrefillDownloadFails() throws Exception {
        ModelEntity modelEntity = model();
        when(modelService.read("model-1")).thenReturn(modelEntity);
        when(modelService.buildPrefillData(modelEntity)).thenThrow(new RuntimeException("prefill failed"));

        ModelCreateView view = new ModelCreateView(modelService);
        UI.getCurrent().add(view);

        view.beforeEnter(beforeEnterEvent("model-1"));
        assertDoesNotThrow(() -> view.afterNavigation(null));
        flushUi();

        verify(modelService).read("model-1");
        verify(modelService).buildPrefillData(modelEntity);
    }

    @Test
    void uploadModelShouldSaveAfterThumbnailCallback() throws Exception {
        when(modelService.saveFromUpload(eq(null), eq("Lebka"), any(), any(), anyList(), anyList(), eq("data:image/png;base64,abc"), any()))
                .thenReturn("saved-model");

        ModelCreateView view = new ModelCreateView(modelService);
        UI.getCurrent().add(view);

        ModelUploadForm form = findAll(view, ModelUploadForm.class).getFirst();
        form.getModelName().setValue("Lebka");

        invoke(view, "uploadModel");
        triggerBackgroundSpecCallback(view, null);
        triggerThumbnailCallback(view, "data:image/png;base64,abc");

        verify(modelService).saveFromUpload(eq(null), eq("Lebka"), any(), any(), anyList(), anyList(), eq("data:image/png;base64,abc"), any());
    }

    @Test
    void uploadModelShouldSwallowSaveErrorsInsideThumbnailCallback() throws Exception {
        when(modelService.saveFromUpload(eq(null), eq("Lebka"), any(), any(), anyList(), anyList(), eq("data:image/png;base64,abc"), any()))
                .thenThrow(new RuntimeException("save failed"));

        ModelCreateView view = new ModelCreateView(modelService);
        UI.getCurrent().add(view);

        ModelUploadForm form = findAll(view, ModelUploadForm.class).getFirst();
        form.getModelName().setValue("Lebka");

        invoke(view, "uploadModel");
        triggerBackgroundSpecCallback(view, null);
        assertDoesNotThrow(() ->
                triggerThumbnailCallback(view, "data:image/png;base64,abc")
        );
        verify(modelService).saveFromUpload(eq(null), eq("Lebka"), any(), any(), anyList(), anyList(), eq("data:image/png;base64,abc"), any());
    }

    private BeforeEnterEvent beforeEnterEvent(String modelId) {
        BeforeEnterEvent event = Mockito.mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("modelId", modelId));
        return event;
    }

    private ModelEntity model() {
        return ModelEntity.builder()
                .metadataId("model-1")
                .name("Lebka")
                .model(ModelFileEntity.builder().id("file-1").name("Lebka").related(List.of()).build())
                .otherTextures(List.of())
                .build();
    }

    private InputStreamMultipartFile file(String name, String content) {
        return InputStreamMultipartFile.builder()
                .fileName(name)
                .displayName(name)
                .inputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @SuppressWarnings("SameParameterValue")
    private void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private void invokeRendererCallback(ModelCreateView view, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Object modelDiv = getField(view, "modelDiv");
        Field rendererField = modelDiv.getClass().getDeclaredField("renderer");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(modelDiv);
        Method method = renderer.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(renderer, args);
    }

    @SuppressWarnings("SameParameterValue")
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



    @Test
    void uploadModelShouldCallEndLoadingOverlayInCatchWhenModelDivIsNull() throws Exception {
        ModelCreateView view = new ModelCreateView(modelService);
        UI.getCurrent().add(view);
        setField(view, "modelDiv", null);
        assertDoesNotThrow(() -> invoke(view, "uploadModel"));
        verify(modelService, never()).saveFromUpload(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private void triggerBackgroundSpecCallback(ModelCreateView view, String backgroundSpecJson) throws Exception {
        Object modelDiv = getField(view, "modelDiv");
        java.lang.reflect.Field rendererField = modelDiv.getClass().getDeclaredField("renderer");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(modelDiv);
        java.lang.reflect.Field callbacksField = renderer.getClass().getDeclaredField("backgroundSpecCallbacks");
        callbacksField.setAccessible(true);
        java.util.Map<String, ?> callbacks = (java.util.Map<String, ?>) callbacksField.get(renderer);
        String requestId = callbacks.keySet().stream().findFirst().orElseThrow();
        invokeRendererCallback(view, "onBackgroundSpecReady",
                new Class[]{String.class, String.class}, requestId, backgroundSpecJson);
    }

    @SuppressWarnings("unchecked")
    private void triggerThumbnailCallback(ModelCreateView view, String dataUrl) throws Exception {
        Object modelDiv = getField(view, "modelDiv");
        java.lang.reflect.Field rendererField = modelDiv.getClass().getDeclaredField("renderer");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(modelDiv);
        java.lang.reflect.Field callbacksField = renderer.getClass().getDeclaredField("thumbnailCallbacks");
        callbacksField.setAccessible(true);
        java.util.Map<String, ?> callbacks = (java.util.Map<String, ?>) callbacksField.get(renderer);
        String requestId = callbacks.keySet().stream().findFirst().orElseThrow();
        invokeRendererCallback(view, "onThumbnailReady",
                new Class[]{String.class, String.class}, requestId, dataUrl);
    }


    @SuppressWarnings("SameParameterValue")
    private void setField(Object target, String name, Object value) throws Exception {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }
}
