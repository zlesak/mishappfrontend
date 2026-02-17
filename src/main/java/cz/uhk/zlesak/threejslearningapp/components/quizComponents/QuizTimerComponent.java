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
 * Component for displaying and managing quiz countdown timer.
 * Supports auto-submission when time expires with early warning notifications.
 */
public class QuizTimerComponent extends Div implements I18nAware {

    private final Span timerDisplay;
    private final Integer timeLimitMinutes;
    private int remainingTimeSeconds;
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
        this.timeLimitMinutes = timeLimitMinutes;
        this.remainingTimeSeconds = timeLimitMinutes != null && timeLimitMinutes > 0 ? (timeLimitMinutes * 60) - 1 : 0;
        this.currentUI = UI.getCurrent();

        timerContainer = new Div();
        timerDisplay = new Span();
        timerContainer.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE, LumoUtility.TextColor.PRIMARY);

        HorizontalLayout timerLayout = new HorizontalLayout(new Icon(VaadinIcon.CLOCK), new Span(text("quiz.timer.remaining")), timerDisplay);
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
     * Updates the timer display every second and handles auto-submission when time expires.
     * Shows a warning notification 5 seconds before auto-submission.
     * Ensures thread safety by using UI.access() for UI updates from the timer thread.
     * If the timer expires, it calls the onTimeExpired callback and shows a final notification to the user.
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

                        if (remainingTimeSeconds <= 5 && !warningShown) {
                            showAutoSubmitWarning();
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
     * Updates the timer display with current remaining time.
     * Formats time as MM:SS and changes color based on remaining time thresholds.
     */
    private void updateTimerDisplay() {
        if (!hasTimeLimit()) return;

        String timeText = String.format("%02d:%02d", remainingTimeSeconds / 60, remainingTimeSeconds % 60);
        timerDisplay.setText(timeText);

        if (remainingTimeSeconds <= 60) {
            timerDisplay.addClassNames(LumoUtility.TextColor.ERROR);
        } else if (remainingTimeSeconds <= 300) {
            timerDisplay.addClassNames(LumoUtility.TextColor.WARNING);
        } else {
            timerDisplay.addClassNames(LumoUtility.TextColor.PRIMARY);
        }
    }

    /**
     * Shows warning notification before auto-submit.
     */
    private void showAutoSubmitWarning() {
        if (currentUI != null && !currentUI.isClosing()) {
            currentUI.access(() -> new WarningNotification(text("quiz.timer.autoSubmit.warning")));
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