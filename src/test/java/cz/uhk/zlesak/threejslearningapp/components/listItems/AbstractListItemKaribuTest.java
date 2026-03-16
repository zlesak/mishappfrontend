package cz.uhk.zlesak.threejslearningapp.components.listItems;

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

import java.util.concurrent.atomic.AtomicInteger;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class AbstractListItemKaribuTest {
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
    void listViewMode_shouldOnlyExposeOpenAction() {
        AbstractListItem item = new AbstractListItem(true, false, VaadinIcon.BOOK);
        UI.getCurrent().add(item);

        assertTrue(button(item, "Otevřít").isVisible());
        assertFalse(button(item, "Vybrat").isVisible());
        assertFalse(button(item, "Upravit").isVisible());
        assertFalse(button(item, "Smazat").isVisible());
    }

    @Test
    void selectAndAdminModes_shouldTriggerRegisteredListeners() {
        AbstractListItem item = new AbstractListItem(false, true, VaadinIcon.BOOK);
        UI.getCurrent().add(item);
        AtomicInteger clickedActions = new AtomicInteger();

        item.setSelectButtonClickListener(event -> clickedActions.incrementAndGet());
        item.setOpenButtonClickListener(event -> clickedActions.incrementAndGet());
        item.setEditButtonClickListener(event -> clickedActions.incrementAndGet());
        item.setDeleteButtonClickListener(event -> clickedActions.incrementAndGet());

        button(item, "Vybrat").click();
        button(item, "Otevřít").click();
        button(item, "Upravit").click();
        button(item, "Smazat").click();

        assertTrue(button(item, "Vybrat").isVisible());
        assertTrue(button(item, "Upravit").isVisible());
        assertTrue(button(item, "Smazat").isVisible());
        assertEquals(4, clickedActions.get());
    }

    private Button button(AbstractListItem item, String text) {
        return findAll(item, Button.class).stream()
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElseThrow();
    }
}
