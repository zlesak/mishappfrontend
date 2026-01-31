package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;

import java.util.*;

/**
 * Component for playing/taking a quiz.
 * Displays questions one by one and collects answers.
 */
public class QuizPlayerComponent extends VerticalLayout implements I18nAware {
    @Getter
    private final List<AbstractQuestionData> questions;
    private final VerticalLayout questionsContainer;
    private final Button submitButton;
    private Runnable onSubmit;
    @Getter
    private HashMap<String, AbstractSubmissionData> answers = new HashMap<>();

    public QuizPlayerComponent(List<AbstractQuestionData> questions) {
        super();
        this.questions = questions;
        setSpacing(true);
        setPadding(true);

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
    }

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
    }
}

