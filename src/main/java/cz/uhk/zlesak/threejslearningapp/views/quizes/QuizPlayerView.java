package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.quiz.QuizPlayerComponent;
import cz.uhk.zlesak.threejslearningapp.components.quiz.QuizResultComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractQuizView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;

/**
 * QuizPlayerView Class - Allows users to take quizzes.
 * It fetches quiz data from the backend and displays it using QuizPlayerComponent.
 * After submission, it shows the results using QuizResultComponent.
 */
@Slf4j
@Route("playQuiz/:quizId?")
@Scope("prototype")
@Tag("quiz-player-view")
@PermitAll
public class QuizPlayerView extends AbstractQuizView {
    private final QuizService quizService;
    private QuizPlayerComponent playerComponent;

    /**
     * Constructor for QuizPlayerView.
     * @param quizService service for handling quiz-related operations
     */
    @Autowired
    public QuizPlayerView(QuizService quizService) {
        super("page.title.quizView");
        this.quizService = quizService;
    }

    /**
     * Handles actions to be performed after navigation to this view.
     */
    @Override
    protected void afterNavigationActions() {
        try {
            QuizEntity quiz = quizService.getQuizForStudent(quizId);
            displayQuiz(quiz);
        } catch (Exception e) {
            log.error("Error loading quiz: {}", e.getMessage(), e);
            new ErrorNotification(text("quiz.error.loading") + ": " + e.getMessage(), 5000);
        }
    }

    /**
     * Displays the quiz using QuizPlayerComponent.
     * @param quiz the quiz entity to be displayed
     */
    private void displayQuiz(QuizEntity quiz) {
        playerComponent = new QuizPlayerComponent(quiz.getQuestions());
        playerComponent.setSubmitListener(this::submitQuiz);

        entityContent.add(playerComponent);
    }

    /**
     * Submits the quiz answers for validation and displays the results.
     */
    private void submitQuiz() {
        try {
            if (!playerComponent.isComplete()) {
                new ErrorNotification(text("quiz.error.notComplete"), 3000);
                return;
            }

            playerComponent.disable();
            QuizValidationResult result = quizService.validateAnswers(quizId, new ArrayList<>(playerComponent.getAnswers().values()));

            entityContent.removeAll();
            entityContent.add(new QuizResultComponent(result));

        } catch (Exception e) {
            log.error("Error při odeslání odpovědí kvízu", e);
            new ErrorNotification(text("quiz.error.submit") + ": " + e.getMessage(), 5000);
        }
    }
}
