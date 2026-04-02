package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuizDetailTableComponentTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void shouldShowDescription_whenDescriptionIsNonBlank() {
        QuickQuizEntity quiz = quiz("quiz-1", "My Quiz", "This is the description", 10, "chapter-1");
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        List<String> texts = spanTexts(table);
        assertTrue(texts.contains("This is the description"),
                "Expected description to be rendered when non-blank");
    }

    @Test
    void shouldNotShowDescription_whenDescriptionIsNull() {
        QuickQuizEntity quiz = QuickQuizEntity.builder()
                .id("quiz-2")
                .name("No Desc Quiz")
                .description(null)
                .timeLimit(5)
                .chapterId("chapter-1")
                .build();

        assertDoesNotThrow(() -> new QuizDetailTableComponent(quiz));

        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);
        List<String> texts = spanTexts(table);
        assertFalse(texts.contains("Popis"),
                "Description row should not appear when description is null");
    }

    @Test
    void shouldNotShowDescription_whenDescriptionIsBlank() {
        QuickQuizEntity quiz = QuickQuizEntity.builder()
                .id("quiz-3")
                .name("Blank Desc Quiz")
                .description("   ")
                .timeLimit(5)
                .chapterId(null)
                .build();

        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);
        List<String> texts = spanTexts(table);
        assertFalse(texts.contains("   "),
                "Blank description should not be rendered");
    }

    @Test
    void shouldShowUnlimitedTimeLimit_whenTimeLimitIsNull() {
        QuickQuizEntity quiz = quiz("quiz-4", "No Limit Quiz", "desc", null, null);
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        List<String> texts = spanTexts(table);
        assertTrue(texts.contains("Neomezeně"),
                "Expected 'Neomezeně' when time limit is null");
    }

    @Test
    void shouldShowTimeLimitInMinutes_whenTimeLimitIsSet() {
        QuickQuizEntity quiz = quiz("quiz-5", "Timed Quiz", "desc", 15, "chapter-2");
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        List<String> texts = spanTexts(table);
        assertTrue(texts.stream().anyMatch(t -> t.contains("15") && t.contains("minut")),
                "Expected time limit with 'minut' label");
    }

    @Test
    void shouldShowChapterId_whenChapterIsSet() {
        QuickQuizEntity quiz = quiz("quiz-6", "Chapter Quiz", "desc", 5, "my-chapter");
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        List<String> texts = spanTexts(table);
        assertTrue(texts.contains("my-chapter"),
                "Expected chapter ID to be shown");
    }

    @Test
    void shouldShowNoneChapter_whenChapterIsNull() {
        QuickQuizEntity quiz = quiz("quiz-7", "No Chapter Quiz", "desc", 5, null);
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        List<String> texts = spanTexts(table);
        assertTrue(texts.contains("Není vázáno na kapitolu"),
                "Expected 'Není vázáno na kapitolu' when chapterId is null");
    }

    private List<String> spanTexts(QuizDetailTableComponent table) {
        return VaadinTestSupport.findAll(table, Span.class).stream()
                .map(Span::getText)
                .toList();
    }

    private QuickQuizEntity quiz(String id, String name, String description, Integer timeLimit, String chapterId) {
        return QuickQuizEntity.builder()
                .id(id)
                .name(name)
                .description(description)
                .timeLimit(timeLimit)
                .chapterId(chapterId)
                .build();
    }
}

