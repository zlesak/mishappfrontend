package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.WarningNotification;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The quiz countdown, with auto-submission when the time runs out.
 *
 * <p>Counted down in minutes rather than seconds. The display used to change every second inside what
 * is now a live region, which makes a screen reader read the clock aloud continuously and drowns out
 * the question. The internal clock still ticks once a second — auto-submission needs that precision —
 * but the text only changes when the minute does.
 *
 * <p>No extension is offered. A time limit on an examination falls under the "essential" exception to
 * WCAG 2.2.1: extending it would destroy what is being measured. What is owed instead is warning, and
 * the warning comes a minute before the end rather than five seconds before it.
 */
public class QuizTimerComponent extends Div implements I18nAware {

    /** How long before the end to warn, in seconds. */
    private static final int WARNING_THRESHOLD_SECONDS = 60;

    private final Span timerDisplay;
    private final Integer timeLimitMinutes;
    private int remainingTimeSeconds;
    private int displayedMinutes = -1;
    private ScheduledExecutorService timerScheduler;
    private boolean timerExpired = false;
    private boolean warningShown = false;
    private final UI currentUI;

    @Setter
    private Runnable onTimeExpired;

    @Getter
    private final Div timerContainer;

    /**
     * Constructor - Initializes the QuizTimerComponent with a specified time limit in minutes.
     * If time limit is set, it starts the countdown timer immediately.
     *
     * @param timeLimitMinutes The time limit for the quiz in minutes. If null or <= 0, timer is not started.
     */
    public QuizTimerComponent(Integer timeLimitMinutes) {
        this(timeLimitMinutes, null);
    }

    /**
     * Counts down the time this attempt has left rather than the quiz's whole limit.
     *
     * <p>The two differ as soon as the student comes back to a quiz they already started: the
     * attempt keeps running on the server, so starting the clock at the full limit again would
     * promise time that is not there and then refuse the submission as late.
     *
     * @param timeLimitMinutes the quiz's limit in minutes; null or &lt;= 0 means no timer at all.
     * @param remainingSeconds seconds left of this attempt, or {@code null} to count the full
     *                         limit, which is the right thing when the attempt starts here.
     */
    public QuizTimerComponent(Integer timeLimitMinutes, Integer remainingSeconds) {
        this.timeLimitMinutes = timeLimitMinutes;
        boolean timed = timeLimitMinutes != null && timeLimitMinutes > 0;
        int startFrom = remainingSeconds != null ? remainingSeconds : (timed ? timeLimitMinutes * 60 : 0);
        this.remainingTimeSeconds = timed ? Math.max(startFrom - 1, 0) : 0;
        this.currentUI = UI.getCurrent();

        timerContainer = new Div();
        timerDisplay = new Span();
        timerContainer.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE, LumoUtility.TextColor.PRIMARY);
        // The role a countdown has. It makes the remaining time available on demand without the
        // screen reader announcing every change on its own.
        timerContainer.getElement().setAttribute("role", "timer");
        timerContainer.getElement().setAttribute("aria-label", text("quiz.timer.remaining"));

        Icon clock = new Icon(VaadinIcon.CLOCK);
        clock.getElement().setAttribute("aria-hidden", "true");
        HorizontalLayout timerLayout = new HorizontalLayout(clock, new Span(text("quiz.timer.remaining")), timerDisplay);
        timerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        timerLayout.setSpacing(true);
        timerContainer.add(timerLayout);

        timerContainer.setVisible(hasTimeLimit());

        if (hasTimeLimit()) {
            startTimer();
        }
    }

    /**
     * Checks if quiz has a time limit set.
     *
     * @return true if time limit is set and greater than 0, false otherwise
     */
    private boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    /**
     * Starts the countdown timer using server push.
     * Ticks once a second so that auto-submission happens on time, but only rewrites the display when
     * the whole minute changes. Warns once, a minute before the end.
     */
    private void startTimer() {
        if (!hasTimeLimit()) return;

        updateTimerDisplay();

        timerScheduler = Executors.newScheduledThreadPool(1);
        timerScheduler.scheduleAtFixedRate(() -> {
            if (currentUI != null && !currentUI.isClosing()) {
                currentUI.access(() -> {
                    if (!timerExpired) {
                        remainingTimeSeconds--;
                        updateTimerDisplay();

                        if (remainingTimeSeconds <= WARNING_THRESHOLD_SECONDS && !warningShown) {
                            showExpiryWarning();
                            warningShown = true;
                        }

                        if (remainingTimeSeconds <= 1) {
                            handleTimeExpired();
                        }
                    }
                });
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Rewrites the display only when the number of whole minutes left has changed, and colours it by
     * how little is left. Under a minute it says so in words instead of showing a shrinking number.
     */
    private void updateTimerDisplay() {
        if (!hasTimeLimit()) return;

        int minutesLeft = (int) Math.ceil(remainingTimeSeconds / 60.0);
        if (minutesLeft == displayedMinutes) {
            return;
        }
        displayedMinutes = minutesLeft;

        timerDisplay.setText(minutesLeft <= 1
                ? text("quiz.timer.lessThanMinute")
                : minutesLeft + " " + text("quiz.timer.minutes"));
        timerDisplay.removeClassNames(
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.TextColor.WARNING,
                LumoUtility.TextColor.ERROR
        );

        if (minutesLeft <= 1) {
            timerDisplay.addClassNames(LumoUtility.TextColor.ERROR);
        } else if (minutesLeft <= 5) {
            timerDisplay.addClassNames(LumoUtility.TextColor.WARNING);
        } else {
            timerDisplay.addClassNames(LumoUtility.TextColor.PRIMARY);
        }
    }

    /**
     * Warns that the time is nearly up, while there is still time to do something about it.
     */
    private void showExpiryWarning() {
        if (currentUI != null && !currentUI.isClosing()) {
            currentUI.access(() -> new WarningNotification(text("quiz.timer.lastMinute.warning")));
        }
    }

    /**
     * Handles timer expiration - calls callback and shows notification.
     */
    private void handleTimeExpired() {
        if (timerExpired) return;

        timerExpired = true;
        timerDisplay.setText(text("quiz.timer.expired"));
        timerDisplay.addClassNames(LumoUtility.TextColor.ERROR);

        stopTimer();

        if (currentUI != null && !currentUI.isClosing()) {
            currentUI.access(() -> new ErrorNotification(text("quiz.timer.expired.submitted")));
        }

        if (onTimeExpired != null) {
            onTimeExpired.run();
        }
    }

    /**
     * Stops the timer.
     */
    public void stopTimer() {
        if (timerScheduler != null && !timerScheduler.isShutdown()) {
            timerScheduler.shutdown();
        }
    }

    /**
     * Ensures timer is stopped when component is detached to prevent memory leaks.
     */
    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        stopTimer();
    }
}
