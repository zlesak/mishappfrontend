package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.clearCurrentUi;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findButtonByText;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findFirst;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.setCurrentUi;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityRowTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void theEntityNameIsAHeadingSoItCanBeJumpedTo() {
        TestRow row = new TestRow();
        row.name("Kosti lebky");

        H2 heading = findFirst(row, H2.class);
        assertEquals("Kosti lebky", heading.getText());
    }

    @Test
    void metadataIsLabelledEvenOnANarrowScreen() {
        // The card this replaces hid the labels below the widest breakpoint, which left a bare date
        // with nothing saying whether it was the creation or the modification date.
        TestRow row = new TestRow();
        row.meta("Autor", "Jan Novák");
        row.meta("Vytvořeno", "1. 3. 2026");

        List<String> texts = findAll(row, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.contains("Autor: "), texts.toString());
        assertTrue(texts.contains("Jan Novák"), texts.toString());
        assertTrue(texts.contains("Vytvořeno: "), texts.toString());
    }

    @Test
    void aBlankValueAddsNoMetadataAtAll() {
        TestRow row = new TestRow();
        int before = findAll(row, Span.class).size();

        row.meta("Autor", null);
        row.meta("Autor", "   ");

        assertEquals(before, findAll(row, Span.class).size());
    }

    @Test
    void theNameIsAllowedToWrapRatherThanBeingCut() {
        TestRow row = new TestRow();
        row.name("Velmi dlouhý název kapitoly, který by se do dlaždice nikdy nevešel");

        H2 heading = findFirst(row, H2.class);
        assertEquals(null, heading.getStyle().get("text-overflow"));
        assertEquals(null, heading.getStyle().get("white-space"));
    }

    @Test
    void theRowIsAtLeastAsTallAsTheDesignRequires() {
        TestRow row = new TestRow();

        assertEquals("68px", row.getStyle().get("min-height"));
        assertEquals("1px solid var(--lumo-contrast-10pct)", row.getStyle().get("border-bottom"));
    }

    @Test
    void theTypeIconIsHiddenFromScreenReaders() {
        // It repeats what the surrounding text already says.
        TestRow row = new TestRow();

        Icon icon = findFirst(row, Icon.class);
        assertEquals("true", icon.getElement().getAttribute("aria-hidden"));
    }

    @Test
    void destructiveAndSecondaryActionsHaveABoundaryOfTheirOwn() {
        // Both used to be a bare word: the delete button was told apart from edit only by the colour
        // of its text, which is the one distinction a colour-blind reader cannot make.
        EntityRow row = new EntityRow(true, true, VaadinIcon.BOOK);

        Button delete = findButtonByText(row, "Smazat");
        Button edit = findButtonByText(row, "Upravit");

        assertEquals("1px solid var(--lumo-error-color)", delete.getStyle().get("border"));
        assertEquals("1px solid var(--lumo-contrast-30pct)", edit.getStyle().get("border"));
        assertEquals("40px", delete.getStyle().get("min-height"));
        assertEquals("40px", edit.getStyle().get("min-height"));
    }

    @Test
    void aLeadingVisualReplacesTheIconRatherThanJoiningIt() {
        TestRow row = new TestRow();
        Div thumbnail = new Div();

        row.leading(thumbnail);

        assertTrue(findAll(row, Icon.class).isEmpty());
        assertNotNull(thumbnail.getParent().orElse(null));
    }

    private static final class TestRow extends EntityRow {
        TestRow() {
            super(true, false, VaadinIcon.BOOK);
        }

        void name(String title) {
            setRowTitle(title);
        }

        void meta(String label, String value) {
            addMetadata(label, value);
        }

        void leading(com.vaadin.flow.component.Component visual) {
            setLeadingVisual(visual);
        }
    }
}
