package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.DescriptionList;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void theParametersArePairedInTheMarkupNotJustOnScreen() {
        // It was a stack of Divs each holding two Spans pushed apart with justify-content: between —
        // a table to look at, and to a screen reader five labels and five values with nothing saying
        // which value belonged to which label.
        QuickQuizEntity quiz = quiz("quiz-1", "My Quiz", "This is the description", 10, "Kostra");
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        assertEquals("dl", table.getElement().getTag());

        List<String> terms = termTexts(table);
        List<String> values = descriptionTexts(table);
        assertEquals(terms.size(), values.size());
        assertEquals(terms.indexOf("Popis"), values.indexOf("This is the description"));
    }

    @Test
    void shouldShowDescription_whenDescriptionIsNonBlank() {
        QuickQuizEntity quiz = quiz("quiz-1", "My Quiz", "This is the description", 10, "Kostra");
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        assertTrue(descriptionTexts(table).contains("This is the description"),
                "Expected description to be rendered when non-blank");
    }

    @Test
    void shouldNotShowDescription_whenDescriptionIsNull() {
        QuickQuizEntity quiz = QuickQuizEntity.builder()
                .id("quiz-2")
                .name("No Desc Quiz")
                .description(null)
                .timeLimit(5)
                .chapterName("Kostra")
                .build();

        assertDoesNotThrow(() -> new QuizDetailTableComponent(quiz));

        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);
        assertFalse(termTexts(table).contains("Popis"),
                "Description row should not appear when description is null");
    }

    @Test
    void shouldNotShowDescription_whenDescriptionIsBlank() {
        QuickQuizEntity quiz = QuickQuizEntity.builder()
                .id("quiz-3")
                .name("Blank Desc Quiz")
                .description("   ")
                .timeLimit(5)
                .chapterName(null)
                .build();

        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);
        assertFalse(termTexts(table).contains("Popis"),
                "Blank description should not be rendered");
    }

    @Test
    void shouldShowUnlimitedTimeLimit_whenTimeLimitIsNull() {
        QuickQuizEntity quiz = quiz("quiz-4", "No Limit Quiz", "desc", null, null);
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        assertTrue(descriptionTexts(table).contains("Neomezeně"),
                "Expected 'Neomezeně' when time limit is null");
    }

    @Test
    void shouldShowTimeLimitInMinutes_whenTimeLimitIsSet() {
        QuickQuizEntity quiz = quiz("quiz-5", "Timed Quiz", "desc", 15, "Kostra");
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        assertTrue(descriptionTexts(table).stream().anyMatch(t -> t.contains("15") && t.contains("minut")),
                "Expected time limit with 'minut' label");
    }

    @Test
    void theChapterIsNamedNeverIdentified() {
        // The id was printed verbatim: an internal identifier that told the reader nothing.
        QuickQuizEntity quiz = QuickQuizEntity.builder()
                .id("quiz-6")
                .name("Chapter Quiz")
                .description("desc")
                .timeLimit(5)
                .chapterId("6612ab34cd56ef7890123456")
                .chapterName("Kosti lebky")
                .build();
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        List<String> values = descriptionTexts(table);
        assertTrue(values.contains("Kosti lebky"), values.toString());
        assertTrue(values.stream().noneMatch(t -> t.contains("6612ab34")), values.toString());
    }

    @Test
    void shouldShowNoneChapter_whenChapterIsNull() {
        QuickQuizEntity quiz = quiz("quiz-7", "No Chapter Quiz", "desc", 5, null);
        QuizDetailTableComponent table = new QuizDetailTableComponent(quiz);

        assertTrue(descriptionTexts(table).contains("Není vázáno na kapitolu"),
                "Expected 'Není vázáno na kapitolu' when the quiz has no chapter");
    }

    private List<String> termTexts(QuizDetailTableComponent table) {
        return VaadinTestSupport.findAll(table, DescriptionList.Term.class).stream()
                .map(DescriptionList.Term::getText)
                .toList();
    }

    private List<String> descriptionTexts(QuizDetailTableComponent table) {
        return VaadinTestSupport.findAll(table, DescriptionList.Description.class).stream()
                .map(DescriptionList.Description::getText)
                .toList();
    }

    private QuickQuizEntity quiz(String id, String name, String description, Integer timeLimit, String chapterName) {
        return QuickQuizEntity.builder()
                .id(id)
                .name(name)
                .description(description)
                .timeLimit(timeLimit)
                .chapterName(chapterName)
                .build();
    }
}
