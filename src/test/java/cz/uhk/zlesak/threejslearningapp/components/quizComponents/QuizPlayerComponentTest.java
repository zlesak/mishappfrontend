package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OpenTextSubmissionData;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class QuizPlayerComponentTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    private List<AbstractQuestionData> singleQuestion() {
        return List.of(OpenTextQuestionData.builder()
                .questionId("q1")
                .questionText("What is the largest bone?")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .points(5)
                .placeholder("Answer")
                .build());
    }

    @Test
    void constructorShouldBuildComponentWithQuestionsAndUnansweredState() {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        assertNotNull(player);
        assertEquals(1, player.getQuestions().size());
        assertFalse(player.isComplete());
    }

    @Test
    void setSubmitListenerAndClickShouldInvokeListener() throws Exception {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        player.getAnswers().put("q1", answer());
        AtomicBoolean called = new AtomicBoolean(false);
        player.setSubmitListener(() -> called.set(true));

        getSubmitButton(player).click();

        assertTrue(called.get());
    }

    @Test
    void withQuestionsLeftBlankTheFirstSubmitListsThemInsteadOfSending() throws Exception {
        // isComplete() existed but was never called from anywhere, so a quiz could be sent with
        // questions the user had simply scrolled past without noticing.
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        AtomicBoolean called = new AtomicBoolean(false);
        player.setSubmitListener(() -> called.set(true));

        Button submit = getSubmitButton(player);
        submit.click();

        assertFalse(called.get(), "Nothing should have been sent yet");
        assertEquals(List.of(1), player.getUnansweredQuestionNumbers());

        Div summary = getUnansweredSummary(player);
        assertTrue(summary.isVisible());
        assertEquals("status", summary.getElement().getAttribute("role"));
        assertTrue(VaadinTestSupport.findAll(summary, Anchor.class).stream()
                .anyMatch(link -> "#otazka-1".equals(link.getHref())));
        assertEquals("Odeslat i tak", submit.getText());
    }

    @Test
    void aSecondSubmitGoesAheadWithoutTrappingTheUser() throws Exception {
        // Leaving an answer blank is a legitimate choice, and refusing to submit at all would trap
        // someone who cannot answer a question.
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        AtomicBoolean called = new AtomicBoolean(false);
        player.setSubmitListener(() -> called.set(true));

        Button submit = getSubmitButton(player);
        submit.click();
        submit.click();

        assertTrue(called.get());
        assertFalse(getUnansweredSummary(player).isVisible());
    }

    @Test
    void theHeaderCarriesTheCountdownAndTheProgress() throws Exception {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), 10);

        Div header = player.getQuizHeader();
        assertTrue(header.getChildren().anyMatch(child -> child == player.getTimerContainer()));

        ProgressBar progress = VaadinTestSupport.findFirst(header, ProgressBar.class);
        assertEquals(1.0, progress.getMax());
        assertEquals(0.0, progress.getValue());

        // The bar states its own value in words, for anyone who cannot see how full it is.
        Span label = VaadinTestSupport.findAll(header, Span.class).stream()
                .filter(span -> "quiz-progress-label".equals(span.getId().orElse("")))
                .findFirst()
                .orElseThrow();
        assertEquals("Zodpovězeno 0 z 1", label.getText());
        assertEquals(label.getId().orElseThrow(), progress.getElement().getAttribute("aria-labelledby"));

        player.disable();
    }

    private OpenTextSubmissionData answer() {
        return OpenTextSubmissionData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .text("Femur")
                .build();
    }

    private Div getUnansweredSummary(QuizPlayerComponent player) throws Exception {
        Field field = QuizPlayerComponent.class.getDeclaredField("unansweredSummary");
        field.setAccessible(true);
        return (Div) field.get(player);
    }

    @Test
    void submitButtonClickWithNullListenerShouldNotThrow() throws Exception {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        assertDoesNotThrow(() -> getSubmitButton(player).click());
    }

    @Test
    void isCompleteShouldReturnFalseWhenNoAnswers() {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        assertFalse(player.isComplete());
    }

    @Test
    void isCompleteShouldReturnTrueWhenAllQuestionsAnswered() {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        player.getAnswers().put("q1", OpenTextSubmissionData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .text("Femur")
                .build());
        assertTrue(player.isComplete());
    }

    @Test
    void disableShouldDisableSubmitButtonAndQuestionsContainer() throws Exception {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        player.disable();
        assertFalse(getSubmitButton(player).isEnabled());
    }

    @Test
    void enableShouldReEnableSubmitButtonAfterDisable() throws Exception {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        player.disable();
        player.enable();
        assertTrue(getSubmitButton(player).isEnabled());
    }

    @Test
    void theAutomaticSubmissionCommitsTheFieldBeingTypedInBeforePressingSubmit() {
        // The student who runs out of time has their caret in the answer, and a synthetic click
        // does not move focus the way a real one does. Without the blur the answer that was being
        // written is never sent and is graded as unanswered. The script cannot be executed here --
        // there is no browser -- so what is pinned is that the blur is in it and comes first.
        // That the sequence does commit the value was measured separately in a browser; see the
        // note on AUTO_SUBMIT_SCRIPT. This test guards the ordering that measurement relied on.
        int blur = QuizPlayerComponent.AUTO_SUBMIT_SCRIPT.indexOf("blur()");
        int click = QuizPlayerComponent.AUTO_SUBMIT_SCRIPT.indexOf("click()");

        assertTrue(blur >= 0, "The automatic submission does not commit the focused field");
        assertTrue(click > blur, "The field has to be committed before the submit button is pressed");
    }

    @Test
    void constructorWithTimeLimitShouldExposeTimerContainer() {
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), 1);
        assertNotNull(player.getTimerContainer());
        player.disable();
    }

    private Button getSubmitButton(QuizPlayerComponent player) throws Exception {
        Field field = QuizPlayerComponent.class.getDeclaredField("submitButton");
        field.setAccessible(true);
        return (Button) field.get(player);
    }
}

