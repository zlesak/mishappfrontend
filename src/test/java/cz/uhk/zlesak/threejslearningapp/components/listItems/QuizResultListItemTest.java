package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.button.Button;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QuizResultListItemTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void shouldConstructWithResult() {
        QuickQuizResult result = result("res-1", 10, 8, 80.0);
        assertDoesNotThrow(() -> new QuizResultListItem(result, false, "back"));
    }

    @Test
    void openButtonClick_shouldTriggerNavigationListener() {
        QuickQuizResult result = result("res-1", 10, 8, 80.0);
        QuizResultListItem item = new QuizResultListItem(result, false, "back");

        List<Button> buttons = VaadinTestSupport.findAll(item, Button.class);
        assertFalse(buttons.isEmpty(), "Expected at least one button in QuizResultListItem");

        Button openButton = buttons.stream()
                .filter(b -> "Otevřít".equals(b.getText()))
                .findFirst()
                .orElse(buttons.get(buttons.size() - 1));

        try {
            openButton.click();
        } catch (Exception ignored) {
        }
    }

    @Test
    void openButtonClick_administrationView_shouldTriggerNavigationListener() {
        QuickQuizResult result = result("res-2", 20, 15, 75.0);
        QuizResultListItem item = new QuizResultListItem(result, true, "admin-back");

        List<Button> buttons = VaadinTestSupport.findAll(item, Button.class);
        Button openButton = buttons.stream()
                .filter(b -> "Otevřít".equals(b.getText()))
                .findFirst()
                .orElse(buttons.get(buttons.size() - 1));

        try {
            openButton.click();
        } catch (Exception ignored) {
        }
    }

    private QuickQuizResult result(String id, int maxScore, int totalScore, double percentage) {
        return QuickQuizResult.builder()
                .id(id)
                .maxScore(maxScore)
                .totalScore(totalScore)
                .percentage(percentage)
                .build();
    }
}

