package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmDialogTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void confirmButton_shouldRunCallbackAndCloseDialog() {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        ConfirmDialog dialog = new ConfirmDialog("Delete", "Confirm deletion", "Delete now", () -> confirmed.set(true), true);
        dialog.open();

        Button confirmButton = findButtonByText(dialog, "Delete now");
        confirmButton.click();

        assertTrue(confirmed.get());
        assertFalse(dialog.isOpened());
        assertTrue(confirmButton.getThemeNames().contains("primary"));
        assertTrue(confirmButton.getThemeNames().contains("error"));
    }

    @Test
    void createDeleteConfirmation_shouldUseLocalizedChapterTexts() {
        ConfirmDialog dialog = ConfirmDialog.createDeleteConfirmation("chapter", "Anatomie", () -> {
        });

        H2 title = findAll(dialog, H2.class).getFirst();
        Paragraph message = findAll(dialog, Paragraph.class).getFirst();
        Button cancelButton = findButtonByText(dialog, "Zrušit");
        Button confirmButton = findButtonByText(dialog, "Smazat kapitolu");

        assertEquals("Smazat kapitolu", title.getText());
        assertEquals("Opravdu chcete smazat kapitolu \"Anatomie\"?", message.getText());
        assertTrue(cancelButton.getThemeNames().contains("tertiary"));
        assertTrue(confirmButton.getThemeNames().contains("error"));
    }
}
