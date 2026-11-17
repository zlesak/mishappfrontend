package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.clearCurrentUi;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.setCurrentUi;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationComponentTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void constructor_shouldRenderFirstPageAndDisablePreviousButton() {
        PaginationComponent component = new PaginationComponent(0, 10, 50, ignored -> {
        });

        List<Button> buttons = findAll(component, Button.class);

        assertEquals(5, buttons.size());
        assertFalse(buttons.getFirst().isEnabled());
        assertEquals("1", buttons.get(1).getText());
        assertTrue(buttons.getLast().isEnabled());
    }

    @Test
    void theCurrentPageIsMarkedButStaysReachableByKeyboard() {
        // It used to be disabled, which takes it out of the tab order: a keyboard user tabbing along
        // the pager skipped straight past the page they were on and lost their place in the sequence.
        PaginationComponent component = new PaginationComponent(0, 10, 50, ignored -> {
        });

        Button current = pageButton(component, "1");

        assertTrue(current.isEnabled());
        assertEquals("page", current.getElement().getAttribute("aria-current"));
        assertEquals("Stránka 1, aktuální", current.getAriaLabel().orElseThrow());
    }

    @Test
    void clickingThePageYouAreOnChangesNothing() {
        List<Integer> selectedPages = new ArrayList<>();
        PaginationComponent component = new PaginationComponent(0, 10, 50, selectedPages::add);

        pageButton(component, "1").click();

        assertTrue(selectedPages.isEmpty());
    }

    @Test
    void theArrowsSayWhereTheyGoAndAreBigEnoughToHit() {
        PaginationComponent component = new PaginationComponent(1, 10, 50, ignored -> {
        });

        List<Button> buttons = findAll(component, Button.class);
        Button previous = buttons.getFirst();
        Button next = buttons.getLast();

        assertEquals("Předchozí stránka", previous.getAriaLabel().orElseThrow());
        assertEquals("Následující stránka", next.getAriaLabel().orElseThrow());
        assertEquals("44px", previous.getStyle().get("min-width"));
        assertEquals("44px", next.getStyle().get("min-height"));
    }

    @Test
    void theWholePagerIsANamedNavigationRegion() {
        PaginationComponent component = new PaginationComponent(0, 10, 50, ignored -> {
        });

        assertEquals("nav", component.getElement().getTag());
        assertEquals("Stránkování", component.getElement().getAttribute("aria-label"));
    }

    @Test
    void nextButtonClick_shouldMoveToNextZeroBasedPageAndShowEllipsis() {
        List<Integer> selectedPages = new ArrayList<>();
        PaginationComponent component = new PaginationComponent(0, 10, 100, selectedPages::add);

        List<Button> initialButtons = findAll(component, Button.class);
        initialButtons.getLast().click();

        List<Button> updatedButtons = findAll(component, Button.class);
        List<Div> divs = findAll(component, Div.class);

        assertEquals(List.of(1), selectedPages);
        assertTrue(updatedButtons.getFirst().isEnabled());
        assertEquals("2", updatedButtons.get(2).getText());
        assertEquals("page", updatedButtons.get(2).getElement().getAttribute("aria-current"));
        assertTrue(divs.stream().anyMatch(div -> "...".equals(div.getText())));
    }

    @Test
    void theEllipsisIsNotReadAloud() {
        PaginationComponent component = new PaginationComponent(4, 10, 200, ignored -> {
        });

        Div ellipsis = findAll(component, Div.class).stream()
                .filter(div -> "...".equals(div.getText()))
                .findFirst()
                .orElseThrow();

        assertEquals("true", ellipsis.getElement().getAttribute("aria-hidden"));
        assertNull(ellipsis.getElement().getAttribute("role"));
    }

    @Test
    void itSaysHowMuchOfTheResultSetIsOnScreen() {
        PaginationComponent component = new PaginationComponent(1, 10, 42, ignored -> {
        });

        assertTrue(rangeTexts(component).contains("Zobrazeno 11–20 z 42"), rangeTexts(component).toString());
    }

    @Test
    void theLastPageDoesNotClaimMoreItemsThanExist() {
        PaginationComponent component = new PaginationComponent(4, 10, 42, ignored -> {
        });

        assertTrue(rangeTexts(component).contains("Zobrazeno 41–42 z 42"), rangeTexts(component).toString());
    }

    @Test
    void anEmptyResultSaysSoRatherThanCountingFromOne() {
        PaginationComponent component = new PaginationComponent(0, 10, 0, ignored -> {
        });

        assertTrue(rangeTexts(component).contains("Nenalezena žádná položka"), rangeTexts(component).toString());
    }

    @Test
    void aPagePastTheEndGivesTheCountInsteadOfABackwardsRange() {
        // Page index 2 of a listing that now holds only 11 items: the range would read "21-11".
        PaginationComponent component = new PaginationComponent(2, 10, 11, ignored -> {
        });

        assertTrue(rangeTexts(component).contains("Na této stránce nic není, celkem 11"),
                rangeTexts(component).toString());
    }

    @Test
    void theUserChoosesHowManyItemsAPageHolds() {
        PaginationComponent component = new PaginationComponent(0, 10, 42, ignored -> {
        });
        List<Integer> chosen = new ArrayList<>();
        component.setOnPageSizeChange(chosen::add);

        @SuppressWarnings("unchecked")
        Select<Integer> pageSize = (Select<Integer>) findAll(component, Select.class).getFirst();
        assertEquals("Na stránku", pageSize.getLabel());
        assertEquals(10, pageSize.getValue());

        pageSize.setValue(50);
        // Only a change the user made counts; the listing sets the value itself when it re-renders.
        assertTrue(chosen.isEmpty());
    }

    private List<String> rangeTexts(PaginationComponent component) {
        return findAll(component, Span.class).stream().map(Span::getText).toList();
    }

    private Button pageButton(PaginationComponent component, String label) {
        return findAll(component, Button.class).stream()
                .filter(button -> label.equals(button.getText()))
                .findFirst()
                .orElseThrow();
    }
}
