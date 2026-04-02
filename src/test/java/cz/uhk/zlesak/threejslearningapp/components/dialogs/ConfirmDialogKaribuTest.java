package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ConfirmDialogKaribuTest {
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
    void createDeleteConfirmation_shouldRenderLocalizedTexts() {
        ConfirmDialog dialog = ConfirmDialog.createDeleteConfirmation("chapter", "Anatomie", () -> {
        });
        UI.getCurrent().add(dialog);
        dialog.open();

        assertEquals("Smazat kapitolu", _get(H2.class, spec -> spec.withText("Smazat kapitolu")).getText());
        assertEquals("Opravdu chcete smazat kapitolu \"Anatomie\"?",
                _get(Paragraph.class, spec -> spec.withText("Opravdu chcete smazat kapitolu \"Anatomie\"?")).getText());
        assertEquals("Zrušit", _get(Button.class, spec -> spec.withText("Zrušit")).getText());
        assertEquals("Smazat kapitolu", _get(Button.class, spec -> spec.withText("Smazat kapitolu")).getText());
    }

    @Test
    void clickingConfirm_shouldRunCallbackAndCloseDialog() {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        ConfirmDialog dialog = new ConfirmDialog("Delete", "Confirm deletion", "Delete now", () -> confirmed.set(true), true);
        UI.getCurrent().add(dialog);
        dialog.open();

        _click(_get(Button.class, spec -> spec.withText("Delete now")));

        assertTrue(confirmed.get());
        assertFalse(dialog.isOpened());
    }
}

