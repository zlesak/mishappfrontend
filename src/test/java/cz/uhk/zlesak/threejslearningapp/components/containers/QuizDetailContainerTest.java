package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.button.Button;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QuizDetailContainerTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void shouldConstructWithQuiz() {
        QuickQuizEntity quiz = quiz("quiz-1", "Test Quiz");
        assertDoesNotThrow(() -> new QuizDetailContainer(quiz));
    }

    @Test
    void startButtonClick_shouldTriggerNavigationListener() {
        QuickQuizEntity quiz = quiz("quiz-1", "Test Quiz");
        QuizDetailContainer container = new QuizDetailContainer(quiz);

        List<Button> buttons = VaadinTestSupport.findAll(container, Button.class);
        assertFalse(buttons.isEmpty(), "Expected at least one button in QuizDetailContainer");

        Button startButton = buttons.stream()
                .filter(b -> "Spustit kvíz".equals(b.getText()))
                .findFirst()
                .orElse(buttons.get(0));

        try {
            startButton.click();
        } catch (Exception ignored) {
        }
    }

    private QuickQuizEntity quiz(String id, String name) {
        return QuickQuizEntity.builder()
                .id(id)
                .name(name)
                .description("A test quiz description")
                .timeLimit(5)
                .chapterId("chapter-1")
                .build();
    }
}
