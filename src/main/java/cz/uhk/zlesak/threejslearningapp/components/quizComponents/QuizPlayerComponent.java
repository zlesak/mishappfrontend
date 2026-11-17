package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.StatusBadge;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Component for playing/taking a quiz.
 * Displays questions one by one and collects answers.
 * Uses QuizTimerComponent for time limit functionality.
 */
public class QuizPlayerComponent extends VerticalLayout implements I18nAware {
    /**
     * Commits the field the student is in before pressing submit for them.
     * Exposed for the test that pins this ordering, which is the whole point of the script: a
     * server-side test cannot run it, so it checks that the blur is there and comes first.
     *
     * <p>{@code document.activeElement} is the answer's own {@code <textarea>}, not the
     * {@code <vaadin-text-area>} around it: Vaadin slots the field into light DOM, so nothing here
     * crosses a shadow boundary and the blur needs no walk through shadow roots to find its
     * target. Measured in the browser, with the caret in the field: active element
     * {@code TEXTAREA[slot=textarea]} before, {@code BODY} after.
     *
     * <p>That the sequence also commits the value was measured rather than assumed. On a quiz with
     * no time limit, running this script 39 ms after the last keystroke -- well inside the delay
     * the field waits out before reporting on its own -- stored all 26 characters that had been
     * typed.
     */
    static final String AUTO_SUBMIT_SCRIPT = "document.activeElement?.blur(); this.click();";

    @Getter
    private final List<AbstractQuestionData> questions;
    private final VerticalLayout questionsContainer;
    private final Button submitButton;
    private final QuizTimerComponent timerComponent;
    private final ProgressBar progressBar = new ProgressBar();
    private final Span progressLabel = new Span();
    private final Div unansweredSummary = new Div();
    /** The header holding the timer and the progress. Placed by the view, statically, above the questions. */
    @Getter
    private final Div quizHeader = new Div();
    private Runnable onSubmit;
    private boolean submitConfirmed = false;
    @Getter
    private HashMap<String, AbstractSubmissionData> answers = new HashMap<>();

    /**
     * Constructor - Initializes the QuizPlayerComponent with a list of questions and an optional time limit.
     *
     * @param questions        List of questions to be displayed in the quiz
     * @param timeLimitMinutes Time limit for the quiz in minutes. If null or <= 0, no timer is shown.
     */
    public QuizPlayerComponent(List<AbstractQuestionData> questions, Integer timeLimitMinutes) {
        this(questions, timeLimitMinutes, null);
    }

    /**
     * Constructor - Initializes the QuizPlayerComponent with a list of questions and an optional time limit.
     *
     * @param questions        List of questions to be displayed in the quiz
     * @param timeLimitMinutes Time limit for the quiz in minutes. If null or <= 0, no timer is shown.
     * @param remainingSeconds Seconds left of the student's attempt, or {@code null} to count the
     *                         full limit. They differ once the student returns to a quiz they
     *                         already started.
     */
    public QuizPlayerComponent(List<AbstractQuestionData> questions, Integer timeLimitMinutes,
                               Integer remainingSeconds) {
        super();
        this.questions = questions;

        setSpacing(true);
        setPadding(true);

        timerComponent = new QuizTimerComponent(timeLimitMinutes, remainingSeconds);

        questionsContainer = new VerticalLayout();
        questionsContainer.setSpacing(true);
        questionsContainer.setPadding(false);
        questionsContainer.setWidthFull();

        submitButton = new Button(text("quiz.submit"));
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitButton.addClickListener(e -> requestSubmit());

        buildHeader();
        buildUnansweredSummary();

        add(quizHeader, questionsContainer, unansweredSummary, submitButton);
        renderQuestions();
        updateProgress();
        setWidthFull();

        // Time is up: submit whatever is there, without stopping to summarise what is missing.
        //
        // The field being typed in is blurred first. A click the user performs moves focus to the
        // button on its own, which is what makes a hand-submitted answer arrive complete; this
        // click is synthetic and leaves focus where it was, so without the blur the last thing
        // the student wrote would not be sent and would be graded as unanswered. Both run in one
        // script so the value reaches the server in the same request as the click.
        timerComponent.setOnTimeExpired(() -> {
            submitConfirmed = true;
            submitButton.getElement().executeJs(AUTO_SUBMIT_SCRIPT);
        });
    }

    /**
     * Builds the quiz header: the countdown and how far through the quiz the user is.
     *
     * <p>Both used to be absent or floating. The timer was positioned with {@code float: right} and
     * {@code position: sticky} over the content, which at 400 % zoom covers the element the user is
     * working in — the one place they need to be able to see.
     */
    private void buildHeader() {
        quizHeader.addClassName("quiz-header");
        quizHeader.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("padding", "var(--lumo-space-s) 0")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        progressBar.setMin(0);
        progressBar.setWidth("14rem");

        progressLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        progressLabel.setId("quiz-progress-label");
        progressBar.getElement().setAttribute("aria-labelledby", progressLabel.getId().orElseThrow());

        HorizontalLayout progress = new HorizontalLayout(progressLabel, progressBar);
        progress.setAlignItems(FlexComponent.Alignment.CENTER);
        progress.setSpacing(true);
        progress.setPadding(false);

        quizHeader.add(timerComponent.getTimerContainer(), progress);
    }

    private void buildUnansweredSummary() {
        unansweredSummary.addClassName("quiz-unanswered");
        unansweredSummary.setVisible(false);
        unansweredSummary.getElement().setAttribute("role", "status");
        unansweredSummary.getElement().setAttribute("tabindex", "-1");
        unansweredSummary.getStyle()
                .set("width", "100%")
                .set("padding", "var(--lumo-space-m)")
                .set("border", "2px solid var(--lumo-error-color)")
                .set("border-radius", "var(--lumo-border-radius-m)");
    }

    /**
     * Renders the questions in the quiz by creating QuestionCardDivComponent for each question.
     * Clears the container before rendering to ensure it reflects the current state of questions and answers.
     */
    private void renderQuestions() {
        questionsContainer.removeAll();
        for (int i = 0; i < questions.size(); i++) {
            questionsContainer.add(new QuestionCardDivComponent(
                    questions.get(i), i + 1, questions.size(), answers, this::updateProgress));
        }
    }

    /**
     * Restates how many questions are answered. Read from the progress bar's own label, so it is
     * available to someone who cannot see how full the bar is.
     */
    private void updateProgress() {
        int answered = answers.size();
        progressBar.setMax(Math.max(1, questions.size()));
        progressBar.setValue(Math.min(answered, questions.size()));
        progressLabel.setText(text("quiz.progress.answered", answered, questions.size()));
    }

    /**
     * Sets listener for quiz submission.
     *
     * @param listener Runnable to execute on submit
     */
    public void setSubmitListener(Runnable listener) {
        this.onSubmit = listener;
    }

    /**
     * Handles a submission request.
     *
     * <p>With questions still unanswered the first request lists them instead of submitting, each as a
     * link that puts the caret in the question. Pressing submit again goes ahead: leaving a question
     * blank is a legitimate choice, and blocking submission outright would trap someone who cannot
     * answer one at all.
     */
    private void requestSubmit() {
        if (!submitConfirmed && !isComplete()) {
            showUnansweredSummary();
            submitConfirmed = true;
            return;
        }
        unansweredSummary.setVisible(false);
        if (onSubmit != null) {
            onSubmit.run();
        }
    }

    private void showUnansweredSummary() {
        unansweredSummary.removeAll();

        H2 heading = new H2(text("quiz.submit.unanswered.heading"));
        heading.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);

        UnorderedList list = new UnorderedList();
        list.getStyle().set("margin", "var(--lumo-space-s) 0 0").set("padding-left", "1.2em");

        for (int i = 0; i < questions.size(); i++) {
            if (answers.containsKey(questions.get(i).getQuestionId())) {
                continue;
            }
            int number = i + 1;
            Anchor link = new Anchor("#" + QuestionCardDivComponent.QUESTION_ID_PREFIX + number,
                    text("quiz.question.number") + " " + number);
            list.add(new ListItem(link));
        }

        unansweredSummary.add(heading, new StatusBadge(text("quiz.submit.unanswered.badge"), StatusBadge.Tone.WARNING), list);
        unansweredSummary.setVisible(true);
        unansweredSummary.getElement().callJsFunction("focus");

        submitButton.setText(text("quiz.submit.anyway"));
    }

    /**
     * Checks if all questions are answered.
     *
     * @return true if all answered, false otherwise
     */
    public boolean isComplete() {
        return answers.size() == questions.size();
    }

    /**
     * @return numbers of the questions still without an answer, counted from one.
     */
    public List<Integer> getUnansweredQuestionNumbers() {
        List<Integer> unanswered = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            if (!answers.containsKey(questions.get(i).getQuestionId())) {
                unanswered.add(i + 1);
            }
        }
        return unanswered;
    }

    /**
     * Disables the quiz (after submission).
     */
    public void disable() {
        submitButton.setEnabled(false);
        questionsContainer.setEnabled(false);
        timerComponent.stopTimer();
    }

    /**
     * Re-enables the quiz (if submission failed).
     */
    public void enable() {
        submitButton.setEnabled(true);
        questionsContainer.setEnabled(true);
    }

    /**
     * Gets the timer container for external positioning.
     * @return Div containing the timer component
     */
    public Div getTimerContainer() {
        return timerComponent.getTimerContainer();
    }

    /**
     * Stops the timer when the component is detached to prevent memory leaks.
     */
    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        timerComponent.stopTimer();
    }
}
