package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.button.Button;
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
        AtomicBoolean called = new AtomicBoolean(false);
        player.setSubmitListener(() -> called.set(true));

        getSubmitButton(player).click();

        assertTrue(called.get());
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

