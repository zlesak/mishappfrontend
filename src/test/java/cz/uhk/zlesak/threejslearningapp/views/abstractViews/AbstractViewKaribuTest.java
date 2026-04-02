package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.exceptions.ApiCallException;
import cz.uhk.zlesak.threejslearningapp.services.AbstractService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class AbstractViewKaribuTest {

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void showSuccessNotification_shouldNotThrow() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(view::exposeShowSuccess);
    }

    @Test
    void showErrorNotification_withNullSourceAndNullMessage_shouldShowDefaultError() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError(null, (String) null));
    }

    @Test
    void showErrorNotification_withEmptySourceAndEmptyMessage_shouldShowDefaultError() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError("", ""));
    }

    @Test
    void showErrorNotification_withNullSourceAndNonEmptyMessage_shouldShowMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError(null, "Some error"));
    }

    @Test
    void showErrorNotification_withNonEmptySourceAndEmptyMessage_shouldShowSource() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError("Upload failed", ""));
    }

    @Test
    void showErrorNotification_withSourceEndingWithColon_shouldFormatProperly() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError("Upload error:", "Backend error"));
    }

    @Test
    void showErrorNotification_withSourceEndingWithColonSpace_shouldFormatProperly() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError("Upload error: ", "Backend error"));
    }

    @Test
    void showErrorNotification_withNormalSourceAndMessage_shouldCombineWithColon() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError("Upload", "Failed"));
    }

    @Test
    void showErrorNotification_withThrowable_shouldDelegateToResolve() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeShowError("source", new RuntimeException("test error")));
    }

    @Test
    void resolveUserFriendlyErrorMessage_withNull_shouldReturnDefault() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(null);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void resolveUserFriendlyErrorMessage_withRegularException_shouldReturnMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(new RuntimeException("something went wrong"));
        assertEquals("something went wrong", result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException400_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        ApiCallException ex = apiEx(400, null, null);
        String result = view.exposeResolveError(ex);
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException401_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(401, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException403_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(403, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException404_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(404, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException409_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(409, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException413_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(413, "/api/other", null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException413ModelUpload_shouldReturnModelTooLarge() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(413, "/api/model/upload", null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException413ModelUpdate_shouldReturnModelTooLarge() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(413, "/api/model/update", null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException415_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(415, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException422_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(422, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException500_shouldReturnStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(500, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException502_shouldReturnGatewayMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(502, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException503_shouldReturnGatewayMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(503, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallException504_shouldReturnGatewayMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(504, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallExceptionUnknownStatusAndBody_shouldReturnBody() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(418, null, "I am a teapot"));
        assertEquals("I am a teapot", result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallExceptionAndEmptyJsonBody_shouldFallbackToStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(418, null, "{}"));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallExceptionAndEmptyArrayBody_shouldFallbackToStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(418, null, "[]"));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallExceptionAndNullBody_shouldFallbackToStatusMessage() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String result = view.exposeResolveError(apiEx(418, null, null));
        assertNotNull(result);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withApiCallExceptionAndLongBody_shouldTruncate() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        String longBody = "A".repeat(300);
        String result = view.exposeResolveError(apiEx(418, null, longBody));
        assertNotNull(result);
        assertTrue(result.endsWith("..."));
        assertTrue(result.length() < 300);
    }

    @Test
    void resolveUserFriendlyErrorMessage_withWrappedApiCallException_shouldTraverseCauseChain() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        ApiCallException inner = apiEx(404, null, null);
        RuntimeException wrapper = new RuntimeException("wrapper", inner);
        String result = view.exposeResolveError(wrapper);
        assertNotNull(result);
    }

    @Test
    void runAsyncVoid_withSuccessfulRunnable_shouldCallSuccessCallback() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        AtomicBoolean runnableCalled = new AtomicBoolean(false);
        AtomicBoolean successCalled = new AtomicBoolean(false);

        view.exposeRunAsyncVoid(
                () -> runnableCalled.set(true),
                () -> successCalled.set(true),
                err -> {}
        );
        MockVaadin.clientRoundtrip(false);

        assertTrue(runnableCalled.get());
        assertTrue(successCalled.get());
    }

    @Test
    void runAsyncVoid_withThrowingRunnable_shouldCallErrorCallback() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        AtomicReference<Throwable> captured = new AtomicReference<>();

        view.exposeRunAsyncVoid(
                () -> { throw new RuntimeException("boom"); },
                () -> {},
                captured::set
        );
        MockVaadin.clientRoundtrip(false);

        assertNotNull(captured.get());
        assertEquals("boom", captured.get().getMessage());
    }

    @Test
    void executeAsyncWithOverlay_shouldCallSuccessCallback() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        AtomicBoolean successCalled = new AtomicBoolean(false);

        view.exposeExecuteAsync(
                () -> { supplierCalled.set(true); return "result"; },
                ignored -> successCalled.set(true),
                err -> {}
        );
        MockVaadin.clientRoundtrip(false);

        assertTrue(supplierCalled.get());
        assertTrue(successCalled.get());
    }

    @Test
    void runFutureWithOverlay_withCompletedFuture_shouldCallSuccessCallback() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        AtomicBoolean successCalled = new AtomicBoolean(false);

        view.exposeRunFutureWithOverlay(
                CompletableFuture.completedFuture("value"),
                v -> successCalled.set(true),
                err -> {}
        );
        MockVaadin.clientRoundtrip(false);

        assertTrue(successCalled.get());
    }

    @Test
    void runFutureWithOverlay_withFailedFuture_shouldCallErrorCallback() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        AtomicReference<Throwable> captured = new AtomicReference<>();

        view.exposeRunFutureWithOverlay(
                CompletableFuture.failedFuture(new RuntimeException("fail")),
                v -> {},
                captured::set
        );
        MockVaadin.clientRoundtrip(false);

        assertNotNull(captured.get());
        assertEquals("fail", captured.get().getMessage());
    }

    @Test
    void futureWithLoadingOverlay_withCompletedFuture_shouldReturnResult() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        CompletableFuture<String> wrapped = view.exposeFutureWithLoadingOverlay(
                CompletableFuture.completedFuture("hello")
        );

        assertEquals("hello", wrapped.join());
    }

    @Test
    void futureWithLoadingOverlay_withFailedFuture_shouldPropagateError() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        CompletableFuture<String> wrapped = view.exposeFutureWithLoadingOverlay(
                CompletableFuture.failedFuture(new RuntimeException("oops"))
        );

        assertTrue(wrapped.isCompletedExceptionally());
    }

    @Test
    void getPageTitle_shouldReturnNonNullString() {
        TestView view = new TestView();
        String title = view.getPageTitle();
        assertNotNull(title);
    }

    @Test
    void findCurrentAbstractView_withNullUi_shouldReturnNull() {
        assertNull(AbstractView.findCurrentAbstractView(null));
    }

    @Test
    void beginLoadingOverlay_shouldNotThrow() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeBeginLoading());
    }

    @Test
    void endLoadingOverlay_shouldNotThrow() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> view.exposeEndLoading());
    }

    @Test
    void beginAndEndLoadingOverlay_shouldBeSymmetric() {
        TestView view = new TestView();
        UI.getCurrent().add(view);
        assertDoesNotThrow(() -> {
            view.exposeBeginLoading();
            view.exposeEndLoading();
        });
        MockVaadin.clientRoundtrip(false);
    }

    private static ApiCallException apiEx(int status, String request, String body) {
        return new ApiCallException("err", null, request, HttpStatus.valueOf(status), body, null);
    }

    private static final class TestView extends AbstractView<TestService> {
        TestView() {
            super("page.title", new TestService());
        }

        void exposeShowSuccess() {
            showSuccessNotification();
        }

        void exposeShowError(String source, String message) {
            showErrorNotification(source, message);
        }

        void exposeShowError(String source, Throwable t) {
            showErrorNotification(source, t);
        }

        String exposeResolveError(Throwable t) {
            return resolveUserFriendlyErrorMessage(t);
        }

        void exposeRunAsyncVoid(Runnable r, Runnable onSuccess, Consumer<Throwable> onError) {
            runAsyncVoid(r, onSuccess, onError);
        }

        <T> void exposeRunFutureWithOverlay(CompletableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onError) {
            runFutureWithOverlay(future, onSuccess, onError);
        }

        <T> CompletableFuture<T> exposeFutureWithLoadingOverlay(CompletableFuture<T> future) {
            return futureWithLoadingOverlay(future);
        }

        <T> void exposeExecuteAsync(Supplier<T> supplier, Consumer<T> onSuccess, Consumer<Throwable> onError) {
            executeAsyncWithOverlay(supplier, onSuccess, onError);
        }

        void exposeBeginLoading() {
            beginLoadingOverlay(UI.getCurrent());
        }

        void exposeEndLoading() {
            endLoadingOverlay(UI.getCurrent());
        }
    }

    private static final class TestService extends AbstractService<QuickModelEntity, QuickModelEntity, String> {
        TestService() {
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
}
