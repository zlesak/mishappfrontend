package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;

/**
 * Component for playing/taking a quiz.
 * Displays questions one by one and collects answers.
 * Uses QuizTimerComponent for time limit functionality.
 */
public class QuizPlayerComponent extends VerticalLayout implements I18nAware {
    @Getter
    private final List<AbstractQuestionData> questions;
    private final VerticalLayout questionsContainer;
    private final Button submitButton;
    private final QuizTimerComponent timerComponent;
    private Runnable onSubmit;
    @Getter
    private HashMap<String, AbstractSubmissionData> answers = new HashMap<>();

    /**
     * Constructor - Initializes the QuizPlayerComponent with a list of questions and an optional time limit.
     *
     * @param questions        List of questions to be displayed in the quiz
     * @param timeLimitMinutes Time limit for the quiz in minutes. If null or <= 0, no timer is shown.
     */
    public QuizPlayerComponent(List<AbstractQuestionData> questions, Integer timeLimitMinutes) {
        super();
        this.questions = questions;
        
        setSpacing(true);
        setPadding(true);

        timerComponent = new QuizTimerComponent(timeLimitMinutes);
        
        questionsContainer = new VerticalLayout();
        questionsContainer.setSpacing(true);
        questionsContainer.setPadding(false);
        questionsContainer.setWidthFull();

        submitButton = new Button(text("quiz.submit"));
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitButton.addClickListener(e -> {
            if (onSubmit != null) {
                onSubmit.run();
            }
        });

        add(questionsContainer, submitButton);
        renderQuestions();
        setWidthFull();

        timerComponent.setOnTimeExpired(() -> submitButton.getElement().callJsFunction("click"));
    }

    /**
     * Renders the questions in the quiz by creating QuestionCardDivComponent for each question.
     * Clears the container before rendering to ensure it reflects the current state of questions and answers.
     */
    private void renderQuestions() {
        questionsContainer.removeAll();
        for (int i = 0; i < questions.size(); i++) {
            questionsContainer.add(new QuestionCardDivComponent(questions.get(i), i + 1, answers));
        }
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
     * Checks if all questions are answered.
     *
     * @return true if all answered, false otherwise
     */
    public boolean isComplete() {
        return answers.size() == questions.size();
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