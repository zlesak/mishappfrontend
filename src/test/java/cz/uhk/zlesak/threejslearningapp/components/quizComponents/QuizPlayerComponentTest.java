package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OpenTextSubmissionData;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class QuizPlayerComponentTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = VaadinTestSupport.setCurrentUi();
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

    @Test
    void answeringTheLastMissingQuestionTakesTheSummaryAndTheSubmitAnywayLabelAway() throws Exception {
        // #90: the summary was built once, when submit was pressed, so after the student filled
        // in the missing answer it still said the question was missing and the button still
        // offered to send "anyway" -- while the progress above already said everything was done.
        QuizPlayerComponent player = new QuizPlayerComponent(singleQuestion(), null);
        Button submit = getSubmitButton(player);
        submit.click();

        answerByTyping(player, 0, "Femur");

        assertFalse(getUnansweredSummary(player).isVisible());
        assertEquals("Odeslat kvíz", submit.getText());
    }

    @Test
    void answeringOneOfSeveralMissingQuestionsLeavesOnlyTheRestListed() throws Exception {
        QuizPlayerComponent player = new QuizPlayerComponent(twoQuestions(), null);
        Button submit = getSubmitButton(player);
        submit.click();

        answerByTyping(player, 0, "Femur");

        Div summary = getUnansweredSummary(player);
        assertTrue(summary.isVisible());
        assertEquals(List.of("#otazka-2"), VaadinTestSupport.findAll(summary, Anchor.class).stream()
                .map(Anchor::getHref).toList());
        // Still incomplete, so the next press still means "send what is there".
        assertEquals("Odeslat i tak", submit.getText());
    }

    @Test
    void aSecondPressAfterAnsweringSomeStillSubmitsAnyway() throws Exception {
        // Rebuilding the list must not reset the confirmation: the documented flow is that the
        // press after the summary submits, and answering one of two questions does not change that.
        QuizPlayerComponent player = new QuizPlayerComponent(twoQuestions(), null);
        AtomicBoolean called = new AtomicBoolean(false);
        player.setSubmitListener(() -> called.set(true));
        Button submit = getSubmitButton(player);
        submit.click();

        answerByTyping(player, 0, "Femur");
        submit.click();

        assertTrue(called.get());
    }

    @Test
    void onceEverythingIsAnsweredTheConfirmationStartsOver() throws Exception {
        // The confirmation belongs to the summary that was dismissed. This pins a safeguard, not a
        // flow the UI offers today: answers are only ever added (a cleared field still counts as
        // answered), so the entry is removed by hand. Should a question ever become unanswered
        // again, the next press lists it instead of silently sending the quiz.
        QuizPlayerComponent player = new QuizPlayerComponent(twoQuestions(), null);
        AtomicBoolean called = new AtomicBoolean(false);
        player.setSubmitListener(() -> called.set(true));
        Button submit = getSubmitButton(player);
        submit.click();

        answerByTyping(player, 0, "Femur");
        answerByTyping(player, 1, "Skull");
        player.getAnswers().remove("q2");
        submit.click();

        assertFalse(called.get(), "Nothing should have been sent without listing the missing question");
        assertTrue(getUnansweredSummary(player).isVisible());
        assertEquals("Odeslat i tak", submit.getText());
    }

    @Test
    void refreshingTheSummaryDoesNotPullFocusOutOfTheAnswerBeingTyped() throws Exception {
        // An open-text answer reports itself after each pause in typing, not on blur. Focusing the
        // summary on every refresh, as the first press does, would yank the caret out of the field
        // mid-sentence.
        QuizPlayerComponent player = new QuizPlayerComponent(openTextQuestions(2), null);
        ui.add(player);
        getSubmitButton(player).click();

        // callJsFunction is queued until the response is written, so the queue has to be flushed
        // before it can be read. Without that the dump is always empty and this test could not fail.
        // The press itself does move focus to the summary, which is what proves the flush works.
        assertTrue(pendingJavaScript().stream().anyMatch(js -> js.contains("focus")),
                "The first press should move focus to the summary");

        answerByTyping(player, 0, "Femur");

        assertTrue(pendingJavaScript().stream().noneMatch(js -> js.contains("focus")),
                "The refreshed summary took focus away from the answer");
    }

    @Test
    void anAnswerThatLeavesTheSameQuestionsMissingDoesNotRebuildTheSummary() throws Exception {
        // The summary is a role="status" live region. Rebuilding it on every pause in typing, with
        // the same questions still missing, would have a screen reader read the list out again
        // each time the student stops to think.
        QuizPlayerComponent player = new QuizPlayerComponent(openTextQuestions(3), null);
        getSubmitButton(player).click();
        answerByTyping(player, 0, "Femur");
        Div summary = getUnansweredSummary(player);
        ListItem before = VaadinTestSupport.findFirst(summary, ListItem.class);

        answerByTyping(player, 0, "Femur, the thigh bone");

        assertSame(before, VaadinTestSupport.findFirst(summary, ListItem.class),
                "The summary was rebuilt although the missing questions did not change");
        assertEquals(List.of("#otazka-2", "#otazka-3"), VaadinTestSupport.findAll(summary, Anchor.class).stream()
                .map(Anchor::getHref).toList());
    }

    @Test
    void choosingAnOptionForTheLastMissingQuestionAlsoTakesTheSummaryAway() throws Exception {
        // Every question type reports through the same listener; this pins it for one that is not
        // a text field.
        QuizPlayerComponent player = new QuizPlayerComponent(List.of(SingleChoiceQuestionData.builder()
                .questionId("q1")
                .questionText("Which bone is the largest?")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(5)
                .options(List.of("Femur", "Stapes"))
                .build()), null);
        Button submit = getSubmitButton(player);
        submit.click();

        @SuppressWarnings("unchecked")
        RadioButtonGroup<Integer> options = VaadinTestSupport.findFirst(player, RadioButtonGroup.class);
        options.setValue(0);

        assertFalse(getUnansweredSummary(player).isVisible());
        assertEquals("Odeslat kvíz", submit.getText());
    }

    /** Flushes what would go to the browser with the next response and returns the scripts queued in it. */
    private List<String> pendingJavaScript() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
    }

    private List<AbstractQuestionData> twoQuestions() {
        return openTextQuestions(2);
    }

    private List<AbstractQuestionData> openTextQuestions(int count) {
        return IntStream.rangeClosed(1, count)
                .<AbstractQuestionData>mapToObj(n -> OpenTextQuestionData.builder()
                        .questionId("q" + n)
                        .questionText("Question " + n)
                        .type(QuestionTypeEnum.OPEN_TEXT)
                        .points(5)
                        .placeholder("Answer")
                        .build())
                .toList();
    }

    /** Answers through the question's own field, so the change reaches the player the way a student's does. */
    private void answerByTyping(QuizPlayerComponent player, int questionIndex, String text) {
        VaadinTestSupport.findAll(player, TextArea.class).get(questionIndex).setValue(text);
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

