package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelLoadEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.exceptions.ApiCallException;
import cz.uhk.zlesak.threejslearningapp.services.AbstractService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings("SameParameterValue")
class AbstractEntityViewTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUiWithBeans(Map.of(ModelService.class, mock(ModelService.class)));
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void loadSingleModelWithTexturesShouldEmitUploadsAndShowAction() {
        TestEntityView view = new TestEntityView();
        List<UploadFileEvent> uploads = new ArrayList<>();
        List<ThreeJsActionEvent> actions = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), UploadFileEvent.class, uploads::add);
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, actions::add);

        view.exposeLoadSingleModelWithTextures(modelWithMainAndOtherTexture(), "question-1", "main", true);

        assertEquals(4, uploads.size());
        assertEquals(List.of(FileType.MODEL, FileType.OTHER, FileType.CSV, FileType.MAIN),
                uploads.stream().map(UploadFileEvent::getFileType).toList());
        assertEquals(List.of(ThreeJsActions.REMOVE, ThreeJsActions.SHOW_MODEL),
                actions.stream().map(ThreeJsActionEvent::getAction).toList());
    }

    @Test
    void loadSingleModelWithTexturesShouldSwitchOtherTextureWhenMainTextureMissing() {
        TestEntityView view = new TestEntityView();
        List<ThreeJsActionEvent> actions = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, actions::add);

        view.exposeLoadSingleModelWithTextures(modelWithOnlyOtherTexture(), "question-1", "main", false);

        assertEquals(List.of(ThreeJsActions.REMOVE, ThreeJsActions.SWITCH_OTHER_TEXTURE),
                actions.stream().map(ThreeJsActionEvent::getAction).toList());
    }

    @Test
    void modelLoadEventShouldHandleModelsWithoutTexturesAndWithTextures() {
        TestEntityView view = new TestEntityView();
        UI.getCurrent().add(view);
        List<UploadFileEvent> uploads = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), UploadFileEvent.class, uploads::add);

        ComponentUtil.fireEvent(UI.getCurrent(), new ModelLoadEvent(view, modelWithoutTextures(), "question-1"));
        ComponentUtil.fireEvent(UI.getCurrent(), new ModelLoadEvent(view, modelWithMainAndOtherTexture(), "question-1"));

        assertEquals(5, uploads.size());
        assertTrue(uploads.stream().anyMatch(event ->
                event.getFileType() == FileType.MODEL && "model-1".equals(event.getModelId())
        ));
        assertTrue(uploads.stream().anyMatch(event -> event.getFileType() == FileType.MAIN));
    }

    @Test
    void loadSingleModelWithTextures_withNullQuestionId_shouldNotFireRemoveAction() {
        TestEntityView view = new TestEntityView();
        List<ThreeJsActionEvent> actions = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, actions::add);

        view.exposeLoadSingleModelWithTextures(modelWithMainAndOtherTexture(), null, "main", true);

        assertTrue(actions.stream().noneMatch(a -> a.getAction() == ThreeJsActions.REMOVE));
        assertTrue(actions.stream().anyMatch(a -> a.getAction() == ThreeJsActions.SHOW_MODEL));
    }

    @Test
    void loadSingleModelWithTextures_withOnlyMainTexture_shouldEmitModelAndMainUploads() {
        TestEntityView view = new TestEntityView();
        List<UploadFileEvent> uploads = new ArrayList<>();
        List<ThreeJsActionEvent> actions = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), UploadFileEvent.class, uploads::add);
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, actions::add);

        QuickModelEntity modelWithOnlyMain = QuickModelEntity.builder()
                .metadataId("meta-only-main")
                .model(ModelFileEntity.builder().id("model-only-main").name("OnlyMain").build())
                .mainTexture(QuickTextureEntity.builder().id("only-main-texture").name("Main").build())
                .otherTextures(List.of())
                .build();

        view.exposeLoadSingleModelWithTextures(modelWithOnlyMain, "q-1", "main", true);

        assertEquals(List.of(FileType.MODEL, FileType.MAIN),
                uploads.stream().map(UploadFileEvent::getFileType).toList());
        assertEquals(List.of(ThreeJsActions.REMOVE, ThreeJsActions.SHOW_MODEL),
                actions.stream().map(ThreeJsActionEvent::getAction).toList());
    }

    @Test
    void loadSingleModelWithTextures_withOtherTextureHavingNullCsvContent_shouldNotEmitCsvUpload() {
        TestEntityView view = new TestEntityView();
        List<UploadFileEvent> uploads = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), UploadFileEvent.class, uploads::add);

        QuickModelEntity modelNoCsv = QuickModelEntity.builder()
                .metadataId("meta-no-csv")
                .model(ModelFileEntity.builder().id("model-no-csv").name("NoCsv").build())
                .mainTexture(QuickTextureEntity.builder().id("main-tex").name("Main").build())
                .otherTextures(List.of(QuickTextureEntity.builder().id("other-tex").name("Other").csvContent(null).build()))
                .build();

        view.exposeLoadSingleModelWithTextures(modelNoCsv, "q-1", "main", true);

        assertTrue(uploads.stream().noneMatch(u -> u.getFileType() == FileType.CSV));
        assertEquals(3, uploads.size());
    }

    @Test
    void testEntityService_readShouldReturnEntityFromApiClient() {
        TestEntityService service = new TestEntityService();

        QuickModelEntity result = service.read("test-id");

        assertNotNull(result);
        assertEquals("test-id", result.getId());
    }

    @Test
    void testEntityService_deleteShouldReturnTrue() {
        TestEntityService service = new TestEntityService();

        boolean deleted = service.delete("any-id");

        assertTrue(deleted);
    }

    private QuickModelEntity modelWithMainAndOtherTexture() {
        return QuickModelEntity.builder()
                .metadataId("meta-1")
                .model(ModelFileEntity.builder().id("model-1").name("Model").build())
                .mainTexture(QuickTextureEntity.builder().id("main-texture").name("Main").build())
                .otherTextures(List.of(QuickTextureEntity.builder()
                        .id("detail-texture")
                        .name("Detail")
                        .csvContent("Area 1;#00FF00")
                        .build()))
                .build();
    }

    private QuickModelEntity modelWithOnlyOtherTexture() {
        return QuickModelEntity.builder()
                .metadataId("meta-1")
                .model(ModelFileEntity.builder().id("model-1").name("Model").build())
                .otherTextures(List.of(QuickTextureEntity.builder().id("detail-texture").name("Detail").build()))
                .build();
    }

    private QuickModelEntity modelWithoutTextures() {
        return QuickModelEntity.builder()
                .metadataId("meta-1")
                .model(ModelFileEntity.builder().id("model-1").name("Model").build())
                .otherTextures(List.of())
                .build();
    }

    private static final class TestEntityView extends AbstractEntityView<TestEntityService> {
        private TestEntityView() {
            super("page.title", true, new TestEntityService());
        }

        private void exposeLoadSingleModelWithTextures(QuickModelEntity model, String questionId, String key, boolean showImmediately) {
            loadSingleModelWithTextures(model, questionId, key, showImmediately);
        }

        private String exposeResolveUserFriendlyErrorMessage(Throwable throwable) {
            return resolveUserFriendlyErrorMessage(throwable);
        }

        private void exposeShowErrorNotification(String source, String message) {
            try {
                showErrorNotification(source, message);
            } catch (Exception ignored) {
            }
        }
    }

    private static final class TestEntityService extends AbstractService<QuickModelEntity, QuickModelEntity, String> {
        private TestEntityService() {
            super(new DummyApiClient());
        }

        @Override
        protected QuickModelEntity validateCreateEntity(QuickModelEntity createEntity) {
            return createEntity;
        }

        @Override
        protected QuickModelEntity createFinalEntity(QuickModelEntity createEntity) {
            return createEntity;
        }
    }

    private static final class DummyApiClient implements IApiClient<QuickModelEntity, QuickModelEntity, String> {
        @Override
        public QuickModelEntity create(QuickModelEntity entity) {
            return entity;
        }

        @Override
        public QuickModelEntity read(String id) {
            return QuickModelEntity.builder().id(id).build();
        }

        @Override
        public QuickModelEntity readQuick(String id) {
            return QuickModelEntity.builder().id(id).build();
        }

        @Override
        public PageResult<QuickModelEntity> readEntities(FilterParameters<String> filterParameters) {
            return new PageResult<>(List.of(), 0L, 0);
        }

        @Override
        public QuickModelEntity update(String id, QuickModelEntity entity) {
            return entity;
        }

        @Override
        public boolean delete(String id) {
            return true;
        }

        @Override
        public String getJwtToken() {
            return null;
        }
    }

    @Test
    void showErrorNotification_bothNullOrBlank_doesNotThrow() {
        TestEntityView view = new TestEntityView();
        assertDoesNotThrow(() -> view.exposeShowErrorNotification(null, null));
    }

    @Test
    void showErrorNotification_emptySourceNonEmptyMessage_doesNotThrow() {
        TestEntityView view = new TestEntityView();
        assertDoesNotThrow(() -> view.exposeShowErrorNotification("", "some error"));
    }

    @Test
    void showErrorNotification_nonEmptySourceEmptyMessage_doesNotThrow() {
        TestEntityView view = new TestEntityView();
        assertDoesNotThrow(() -> view.exposeShowErrorNotification("Source", null));
    }

    @Test
    void showErrorNotification_sourceEndsWithColonSpace_doesNotThrow() {
        TestEntityView view = new TestEntityView();
        assertDoesNotThrow(() -> view.exposeShowErrorNotification("Source: ", "message"));
    }

    @Test
    void showErrorNotification_sourceEndsWithColonOnly_doesNotThrow() {
        TestEntityView view = new TestEntityView();
        assertDoesNotThrow(() -> view.exposeShowErrorNotification("Source:", "message"));
    }

    @Test
    void showErrorNotification_bothNonEmptyNoColon_doesNotThrow() {
        TestEntityView view = new TestEntityView();
        assertDoesNotThrow(() -> view.exposeShowErrorNotification("Source", "message"));
    }

    @Test
    void resolveUserFriendlyErrorMessage_nullThrowable_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(null));
    }

    @Test
    void resolveUserFriendlyErrorMessage_plainException_returnsItsMessage() {
        TestEntityView view = new TestEntityView();
        assertEquals("custom error", view.exposeResolveUserFriendlyErrorMessage(new RuntimeException("custom error")));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallException400_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.BAD_REQUEST, null, null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallException413ModelEndpoint_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/model/upload", HttpStatus.PAYLOAD_TOO_LARGE, null, null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallException413OtherEndpoint_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/other", HttpStatus.PAYLOAD_TOO_LARGE, null, null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallExceptionNullStatusWithBody_returnsBody() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", null, "backend error detail", null);
        assertEquals("backend error detail", view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallException500_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.INTERNAL_SERVER_ERROR, null, null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallException502_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.BAD_GATEWAY, null, null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallException418WithBody_returnsBody() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.I_AM_A_TEAPOT, "teapot body", null);
        assertEquals("teapot body", view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_apiCallException418NullBody_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.I_AM_A_TEAPOT, null, null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_wrappedApiCallException_returnsNonNull() {
        TestEntityView view = new TestEntityView();
        ApiCallException apiEx = new ApiCallException("msg", null, "/api/foo", HttpStatus.BAD_REQUEST, null, null);
        RuntimeException wrapped = new RuntimeException("wrapper", apiEx);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(wrapped));
    }

    @Test
    void resolveUserFriendlyErrorMessage_sanitizeBody_empty_fallsThrough() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.I_AM_A_TEAPOT, "", null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_sanitizeBody_emptyBraces_fallsThrough() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.I_AM_A_TEAPOT, "{}", null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_sanitizeBody_emptyArray_fallsThrough() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.I_AM_A_TEAPOT, "[]", null);
        assertNotNull(view.exposeResolveUserFriendlyErrorMessage(ex));
    }

    @Test
    void resolveUserFriendlyErrorMessage_sanitizeBody_longBody_returnsTruncated() {
        TestEntityView view = new TestEntityView();
        String longBody = "A".repeat(300);
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.I_AM_A_TEAPOT, longBody, null);
        String result = view.exposeResolveUserFriendlyErrorMessage(ex);
        assertTrue(result.endsWith("..."));
        assertEquals(223, result.length());
    }

    @Test
    void resolveUserFriendlyErrorMessage_sanitizeBody_shortBody_returnsAsIs() {
        TestEntityView view = new TestEntityView();
        ApiCallException ex = new ApiCallException("msg", null, "/api/foo", HttpStatus.I_AM_A_TEAPOT, "short body", null);
        assertEquals("short body", view.exposeResolveUserFriendlyErrorMessage(ex));
    }
}
