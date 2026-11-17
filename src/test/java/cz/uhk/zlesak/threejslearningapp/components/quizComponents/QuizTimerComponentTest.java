package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class QuizTimerComponentTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldInitializeTimerState() throws Exception {
        QuizTimerComponent withoutLimit = new QuizTimerComponent(null);
        QuizTimerComponent withLimit = new QuizTimerComponent(1);
        UI.getCurrent().add(withoutLimit, withLimit);

        assertFalse(withoutLimit.getTimerContainer().isVisible());
        assertEquals("méně než minuta", getTimerDisplay(withLimit).getText());

        withLimit.stopTimer();
    }

    @Test
    void theCountdownIsMeasuredInMinutesNotSeconds() throws Exception {
        // The display used to change every second inside what is now a live region, which makes a
        // screen reader read the clock aloud without pausing and drowns out the question.
        QuizTimerComponent timer = new QuizTimerComponent(20);
        UI.getCurrent().add(timer);
        Span display = getTimerDisplay(timer);

        assertEquals("20 min", display.getText());

        // A second passing inside the same minute leaves the text exactly as it was.
        setField(timer, "remainingTimeSeconds", 1198);
        invoke(timer, "updateTimerDisplay");
        assertEquals("20 min", display.getText());

        setField(timer, "remainingTimeSeconds", 1080);
        invoke(timer, "updateTimerDisplay");
        assertEquals("18 min", display.getText());

        timer.stopTimer();
    }

    @Test
    void countsDownWhatIsLeftOfTheAttemptRatherThanTheWholeLimit() throws Exception {
        // A twenty minute quiz reopened with five minutes to go. Starting from the limit again
        // would promise time the server will not honour and then refuse the submission as late.
        QuizTimerComponent timer = new QuizTimerComponent(20, 5 * 60);
        UI.getCurrent().add(timer);

        assertEquals("5 min", getTimerDisplay(timer).getText());

        timer.stopTimer();
    }

    @Test
    void countsDownTheWholeLimitWhenTheAttemptStartsHere() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(20, null);
        UI.getCurrent().add(timer);

        assertEquals("20 min", getTimerDisplay(timer).getText());

        timer.stopTimer();
    }

    @Test
    void showsNoTimeLeftForAnAttemptThatHasAlreadyRunOut() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(20, 0);
        UI.getCurrent().add(timer);

        assertEquals("méně než minuta", getTimerDisplay(timer).getText());

        timer.stopTimer();
    }

    @Test
    void theTimerAnnouncesItselfAsOne() {
        QuizTimerComponent timer = new QuizTimerComponent(10);
        UI.getCurrent().add(timer);

        assertEquals("timer", timer.getTimerContainer().getElement().getAttribute("role"));
        assertEquals("Zbývající čas:", timer.getTimerContainer().getElement().getAttribute("aria-label"));

        timer.stopTimer();
    }

    @Test
    void timerShouldUpdateDisplayWarnAndExpire() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(2);
        UI.getCurrent().add(timer);
        AtomicBoolean expired = new AtomicBoolean(false);
        timer.setOnTimeExpired(() -> expired.set(true));
        setField(timer, "remainingTimeSeconds", 45);

        invoke(timer, "updateTimerDisplay");
        Span display = getTimerDisplay(timer);
        assertEquals("méně než minuta", display.getText());

        // Warned a minute out rather than five seconds out: five seconds is not enough time to do
        // anything about it.
        invoke(timer, "showExpiryWarning");
        setField(timer, "remainingTimeSeconds", 1);
        invoke(timer, "handleTimeExpired");

        assertTrue(expired.get());
        assertEquals("Čas vypršel!", display.getText());
        assertTrue(display.getClassNames().contains(LumoUtility.TextColor.ERROR));
    }

    @Test
    void stopTimerAndDetachShouldShutdownScheduler() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(1);
        UI.getCurrent().add(timer);
        setField(timer, "timerScheduler", Executors.newSingleThreadScheduledExecutor());

        timer.stopTimer();
        assertTrue(getScheduler(timer).isShutdown());

        setField(timer, "timerScheduler", Executors.newSingleThreadScheduledExecutor());
        invokeOnDetach(timer);
        assertTrue(getScheduler(timer).isShutdown());
    }

    @Test
    void constructor_withNullTimeLimit_shouldHideTimerContainer() {
        QuizTimerComponent timer = new QuizTimerComponent(null);
        UI.getCurrent().add(timer);

        assertFalse(timer.getTimerContainer().isVisible());
    }

    @Test
    void constructor_withZeroTimeLimit_shouldHideTimerContainer() {
        QuizTimerComponent timer = new QuizTimerComponent(0);
        UI.getCurrent().add(timer);

        assertFalse(timer.getTimerContainer().isVisible());
    }

    @Test
    void stopTimer_withNullScheduler_shouldNotThrow() {
        QuizTimerComponent timer = new QuizTimerComponent(null);
        UI.getCurrent().add(timer);

        assertDoesNotThrow(timer::stopTimer);
    }

    @Test
    void updateTimerDisplay_withTimeBetween60And300Seconds_shouldAddWarningClass() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(6);
        UI.getCurrent().add(timer);
        setField(timer, "remainingTimeSeconds", 150);

        invoke(timer, "updateTimerDisplay");

        Span display = getTimerDisplay(timer);
        assertTrue(display.getClassNames().contains(LumoUtility.TextColor.WARNING));
        timer.stopTimer();
    }

    @Test
    void updateTimerDisplay_withTimeAbove300Seconds_shouldAddPrimaryClass() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(10);
        UI.getCurrent().add(timer);
        setField(timer, "remainingTimeSeconds", 400);

        invoke(timer, "updateTimerDisplay");

        Span display = getTimerDisplay(timer);
        assertTrue(display.getClassNames().contains(LumoUtility.TextColor.PRIMARY));
        timer.stopTimer();
    }

    @Test
    void negativeTimeLimit_shouldBehaveLikeNoTimeLimit() {
        QuizTimerComponent timer = new QuizTimerComponent(-1);
        UI.getCurrent().add(timer);

        assertFalse(timer.getTimerContainer().isVisible());
    }

    @Test
    void updateTimerDisplay_withLessThan60Seconds_shouldApplyErrorColor() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(5);
        UI.getCurrent().add(timer);
        setField(timer, "remainingTimeSeconds", 30);

        invoke(timer, "updateTimerDisplay");

        Span display = getTimerDisplay(timer);
        assertTrue(display.getClassNames().contains(LumoUtility.TextColor.ERROR));
        timer.stopTimer();
    }

    @Test
    void handleTimeExpired_whenAlreadyExpired_shouldBeIdempotent() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(1);
        UI.getCurrent().add(timer);
        AtomicInteger callCount = new AtomicInteger(0);
        timer.setOnTimeExpired(callCount::incrementAndGet);

        invoke(timer, "handleTimeExpired");
        invoke(timer, "handleTimeExpired");

        assertEquals(1, callCount.get());
    }

    @Test
    void handleTimeExpired_withNullOnTimeExpired_shouldNotThrow() {
        QuizTimerComponent timer = new QuizTimerComponent(1);
        UI.getCurrent().add(timer);

        assertDoesNotThrow(() -> invoke(timer, "handleTimeExpired"));
        timer.stopTimer();
    }

    private void invoke(QuizTimerComponent timer, String methodName) throws Exception {
        Method method = QuizTimerComponent.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(timer);
    }

    private void invokeOnDetach(QuizTimerComponent timer) throws Exception {
        Method method = QuizTimerComponent.class.getDeclaredMethod("onDetach", com.vaadin.flow.component.DetachEvent.class);
        method.setAccessible(true);
        method.invoke(timer, new com.vaadin.flow.component.DetachEvent(timer));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private java.util.concurrent.ScheduledExecutorService getScheduler(QuizTimerComponent timer) throws Exception {
        var field = QuizTimerComponent.class.getDeclaredField("timerScheduler");
        field.setAccessible(true);
        return (java.util.concurrent.ScheduledExecutorService) field.get(timer);
    }

    private Span getTimerDisplay(QuizTimerComponent timer) throws Exception {
        var field = QuizTimerComponent.class.getDeclaredField("timerDisplay");
        field.setAccessible(true);
        return (Span) field.get(timer);
    }
}

