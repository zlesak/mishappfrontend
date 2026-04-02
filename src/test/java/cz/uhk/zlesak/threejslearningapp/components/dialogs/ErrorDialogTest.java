package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.*;

class ErrorDialogTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructor_shouldRenderTitleMessageAndDetails() {
        ErrorDialog dialog = new ErrorDialog(VaadinIcon.WARNING, "Error Title", "Error message", "Error details");
        UI.getCurrent().add(dialog);

        List<H1> headings = findAll(dialog, H1.class);
        List<Paragraph> paragraphs = findAll(dialog, Paragraph.class);

        assertFalse(headings.isEmpty());
        assertEquals("Error Title", headings.getFirst().getText());

        assertTrue(paragraphs.stream().anyMatch(p -> "Error message".equals(p.getText())));
        assertTrue(paragraphs.stream().anyMatch(p -> "Error details".equals(p.getText())));
    }

    @Test
    void constructor_shouldRenderBackButton() {
        ErrorDialog dialog = new ErrorDialog(VaadinIcon.BAN, "Forbidden", "Access denied", "403");
        UI.getCurrent().add(dialog);

        List<Button> buttons = findAll(dialog, Button.class);
        assertFalse(buttons.isEmpty());
        assertEquals("Zpět na hlavní stránku", buttons.getFirst().getText());
    }

    @Test
    void setMessage_shouldUpdateMessageText() {
        ErrorDialog dialog = new ErrorDialog(VaadinIcon.WARNING, "Error", "Initial message", "Details");
        UI.getCurrent().add(dialog);

        dialog.setMessage("Updated message");

        List<Paragraph> paragraphs = findAll(dialog, Paragraph.class);
        assertTrue(paragraphs.stream().anyMatch(p -> "Updated message".equals(p.getText())));
        assertTrue(paragraphs.stream().noneMatch(p -> "Initial message".equals(p.getText())));
    }
}

