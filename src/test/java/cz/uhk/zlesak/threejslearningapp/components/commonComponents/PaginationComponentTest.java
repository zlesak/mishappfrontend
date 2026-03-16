package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.*;

class PaginationComponentTest {

    @Test
    void constructor_shouldRenderFirstPageAndDisablePreviousButton() {
        PaginationComponent component = new PaginationComponent(0, 10, 50, ignored -> {
        });

        List<Button> buttons = findAll(component, Button.class);

        assertEquals(5, buttons.size());
        assertFalse(buttons.getFirst().isEnabled());
        assertEquals("1", buttons.get(1).getText());
        assertFalse(buttons.get(1).isEnabled());
        assertTrue(buttons.getLast().isEnabled());
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
        assertFalse(updatedButtons.get(2).isEnabled());
        assertTrue(divs.stream().anyMatch(div -> "...".equals(div.getText())));
    }
}
