package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.clearCurrentUi;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findFirst;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.setCurrentUi;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageHeaderTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void theTitleIsTheOnlyH1() {
        PageHeader header = new PageHeader("Kapitoly");

        List<H1> headings = findAll(header, H1.class);
        assertEquals(1, headings.size());
        assertEquals("Kapitoly", headings.getFirst().getText());
    }

    @Test
    void theMetaLineIsALiveRegionThatExistsBeforeItHasText() {
        // A region added at the same moment as its text is not announced: the screen reader has to be
        // watching it already. So the span is in the tree from the start, only hidden.
        PageHeader header = new PageHeader("Kapitoly");

        Span meta = metaSpan(header);
        assertEquals("status", meta.getElement().getAttribute("role"));
        assertEquals("none", meta.getStyle().get("display"));

        header.setMeta("Zobrazeno 10 z 42");

        assertEquals("Zobrazeno 10 z 42", meta.getText());
        assertEquals("inline", meta.getStyle().get("display"));
    }

    @Test
    void aBlankMetaHidesTheLineWithoutRemovingTheRegion() {
        PageHeader header = new PageHeader("Kvízy", "Zobrazeno 5 z 5");

        header.setMeta("   ");

        Span meta = metaSpan(header);
        assertEquals("", meta.getText());
        assertEquals("none", meta.getStyle().get("display"));
        assertEquals("status", meta.getElement().getAttribute("role"));
    }

    @Test
    void theActionSlotStaysHiddenUntilSomethingIsPutInIt() {
        PageHeader header = new PageHeader("Modely");

        assertTrue(findAll(header, Button.class).isEmpty());

        Button create = new Button("Vytvořit model");
        header.setActions(create);

        assertEquals(List.of(create), findAll(header, Button.class));
        assertTrue(create.getParent().orElseThrow().isVisible());

        header.setActions();
        assertTrue(findAll(header, Button.class).isEmpty());
    }

    @Test
    void theHeadingIsRenderedAsAHeaderElement() {
        PageHeader header = new PageHeader("Administrační centrum", "Role: vyučující");

        assertEquals("header", header.getElement().getTag());
        assertFalse(findFirst(header, H1.class).getText().isBlank());
    }

    private static Span metaSpan(PageHeader header) {
        return findAll(header, Span.class).stream()
                .filter(span -> "status".equals(span.getElement().getAttribute("role")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No live region in the header"));
    }
}
