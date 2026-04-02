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
        assertEquals("00:59", getTimerDisplay(withLimit).getText());

        withLimit.stopTimer();
    }

    @Test
    void timerShouldUpdateDisplayWarnAndExpire() throws Exception {
        QuizTimerComponent timer = new QuizTimerComponent(1);
        UI.getCurrent().add(timer);
        AtomicBoolean expired = new AtomicBoolean(false);
        timer.setOnTimeExpired(() -> expired.set(true));
        setField(timer, "remainingTimeSeconds", 5);

        invoke(timer, "updateTimerDisplay");
        Span display = getTimerDisplay(timer);
        assertEquals("00:05", display.getText());

        invoke(timer, "showAutoSubmitWarning");
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

