package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class AbstractViewAsyncTest {
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
    void runAsyncShouldExecuteSupplier() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        AtomicBoolean supplierCalled = new AtomicBoolean(false);

        view.executeAsync(() -> {
            supplierCalled.set(true);
            return "ok";
        }, ignored -> {
        }, ignored -> {
        });

        assertTrue(supplierCalled.get());
    }

    @Test
    void unwrapAsyncErrorShouldReturnCauseForCompletionException() {
        TestView view = new TestView();
        IllegalStateException expected = new IllegalStateException("boom");

        Throwable unwrapped = view.unwrapError(new CompletionException(expected));

        IllegalStateException illegalStateException = assertInstanceOf(IllegalStateException.class, unwrapped);
        assertEquals("boom", illegalStateException.getMessage());
    }

    @Test
    void runAsyncShouldRejectWhenSessionBulkheadIsExhausted() {
        TestView view = new TestView();
        UI.getCurrent().add(view);

        String key = AbstractView.class.getName() + ".sessionIoBulkhead";
        VaadinSession.getCurrent().setAttribute(key, new Semaphore(0, true));

        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        view.executeAsync(() -> {
            supplierCalled.set(true);
            return "never";
        }, ignored -> {
        }, error::set);

        assertFalse(supplierCalled.get());
        assertNotNull(error.get());
        assertTrue(error.get().getMessage().contains("Příliš mnoho paralelních operací"));
    }

    private static final class TestView extends AbstractView<TestService> {
        private TestView() {
            super("page.title", new TestService());
        }

        private <T> void executeAsync(Supplier<T> supplier, Consumer<T> onSuccess, Consumer<Throwable> onError) {
            runAsync(supplier, onSuccess, onError);
        }

        private Throwable unwrapError(Throwable throwable) {
            return unwrapAsyncError(throwable);
        }
    }

    private static final class TestService extends AbstractService<QuickModelEntity, QuickModelEntity, String> {
        private TestService() {
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

