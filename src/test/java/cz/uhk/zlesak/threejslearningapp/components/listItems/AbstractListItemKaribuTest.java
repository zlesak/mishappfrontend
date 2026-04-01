package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class AbstractListItemKaribuTest {
    @Autowired private ApplicationContext applicationContext;

    @BeforeEach void setUp() { KaribuSpringTestSupport.setUp(applicationContext); }
    @AfterEach void tearDown() { KaribuSpringTestSupport.tearDown(); }

    @Test
    void listViewMode_shouldOnlyExposeOpenAction() {
        AbstractListItem item = new AbstractListItem(true, false, VaadinIcon.BOOK);
        UI.getCurrent().add(item);
        assertTrue(btn(item, "Otevřít").isVisible());
        assertFalse(btn(item, "Vybrat").isVisible());
        assertFalse(btn(item, "Upravit").isVisible());
        assertFalse(btn(item, "Smazat").isVisible());
    }

    @Test
    void selectAndAdminModes_shouldTriggerRegisteredListeners() {
        AbstractListItem item = new AbstractListItem(false, true, VaadinIcon.BOOK);
        UI.getCurrent().add(item);
        AtomicInteger cnt = new AtomicInteger();
        item.setSelectButtonClickListener(e -> cnt.incrementAndGet());
        item.setOpenButtonClickListener(e -> cnt.incrementAndGet());
        item.setEditButtonClickListener(e -> cnt.incrementAndGet());
        item.setDeleteButtonClickListener(e -> cnt.incrementAndGet());
        btn(item, "Vybrat").click();
        btn(item, "Otevřít").click();
        btn(item, "Upravit").click();
        btn(item, "Smazat").click();
        assertEquals(4, cnt.get());
    }

    @Test
    void runBackendCallWithOverlay_whenUiIsNull_shouldCallOnError() {
        TestListItem item = new TestListItem();
        UI.setCurrent(null);
        AtomicReference<Throwable> err = new AtomicReference<>();
        item.call(() -> "v", v -> {}, err::set);
        assertNotNull(err.get());
        assertInstanceOf(IllegalStateException.class, err.get());
        assertTrue(err.get().getMessage().contains("UI is not available"));
    }

    @Test
    void runBackendCallWithOverlay_whenSupplierSucceeds_shouldCallOnSuccess() {
        TestListItem item = new TestListItem();
        UI.getCurrent().add(item);
        AtomicReference<String> result = new AtomicReference<>();
        item.call(() -> "success-value", result::set, e -> {});
        MockVaadin.clientRoundtrip(false);
        assertEquals("success-value", result.get());
    }

    @Test
    void runBackendCallWithOverlay_whenSupplierThrows_shouldCallOnError() {
        TestListItem item = new TestListItem();
        UI.getCurrent().add(item);
        AtomicReference<Throwable> err = new AtomicReference<>();
        item.call(() -> { throw new RuntimeException("supplier failure"); }, v -> {}, err::set);
        MockVaadin.clientRoundtrip(false);
        assertNotNull(err.get());
        assertEquals("supplier failure", err.get().getMessage());
    }

    @Test
    void runBackendCallWithOverlay_whenUiClosingDuringSupplier_shouldSkipCallback() {
        TestListItem item = new TestListItem();
        UI ui = UI.getCurrent();
        ui.add(item);
        AtomicBoolean called = new AtomicBoolean();
        item.call(() -> { ui.close(); return "v"; }, v -> called.set(true), e -> {});
        assertFalse(called.get());
    }

    private Button btn(AbstractListItem item, String text) {
        return findAll(item, Button.class).stream().filter(c -> text.equals(c.getText())).findFirst().orElseThrow();
    }

    private static final class TestListItem extends AbstractListItem {
        TestListItem() { super(true, false, VaadinIcon.BOOK); }
        <T> void call(Supplier<T> s, Consumer<T> ok, Consumer<Throwable> err) {
            runBackendCallWithOverlay(s, ok, err);
        }
    }
}
