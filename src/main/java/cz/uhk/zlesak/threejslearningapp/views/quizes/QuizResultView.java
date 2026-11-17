package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractQuizView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * QuizResultView Class - Displays the results of a completed quiz.
 * It fetches quiz result data from the backend and displays it using QuizResultComponent.
 */
@Slf4j
@Route("quiz-result/:quizId?/:back?")
@Scope("prototype")
@Tag("quiz-detail")
@PermitAll
public class QuizResultView extends AbstractQuizView {
    private final QuizResultService quizResultService;

    private record QuizResultViewData(QuizValidationResult result, QuizEntity quiz, int possibleScore) {
    }

    /**
     * Constructor for QuizResultView.
     *
     * @param quizResultService the quiz result service
     */
    @Autowired
    public QuizResultView(QuizResultService quizResultService) {
        super("page.title.quizView");
        this.quizResultService = quizResultService;
    }

    /**
     * Handles actions to be performed after navigation to this view.
     *
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        runAsync(
                () -> {
                    QuizValidationResult result = quizResultService.read(quizId);
                    String targetQuizId = redirect == null ? quizId : redirect;
                    // Deliberately not getQuizForStudent: that one starts an attempt, and opening a
                    // past result would then reset the timer of a quiz the student has open.
                    QuizEntity quiz = quizIfStillThere(targetQuizId);
                    // A result records its own score and every question it was graded on, so it
                    // stays readable once the quiz behind it is gone. Insisting on the live quiz
                    // turned every past attempt at a deleted quiz into an error screen.
                    int possibleScore = quiz != null ? QuizService.maxScoreOf(quiz) : result.getMaxScore();
                    return new QuizResultViewData(result, quiz, possibleScore);
                },
                data -> displayQuizResultDetails(data.result(), data.quiz(), data.possibleScore()),
                error -> {
                    log.error("Error loading quiz result: {}", error.getMessage(), error);
                    showErrorNotification(text("quiz.error.loading"), error);
                }
        );
    }

    /**
     * Reads the quiz a result belongs to, tolerating its absence.
     *
     * @param targetQuizId id of the quiz.
     * @return the quiz, or {@code null} when it has been deleted since the attempt was graded.
     */
    private QuizEntity quizIfStillThere(String targetQuizId) {
        try {
            return service.getQuiz(targetQuizId);
        } catch (RuntimeException e) {
            log.info("Kvíz {} k výsledku {} už neexistuje, zobrazuji uložený výsledek.", targetQuizId, quizId);
            return null;
        }
    }

    /**
     * Overridden beforeEnter function to check if the quizId and back parameters are present in the URL.
     * If not, it redirects the user to the QuizListingView.
     *
     * @param event before navigation event with event details
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        if (parameters.getParameterNames().isEmpty() || parameters.get("quizId").isEmpty() || parameters.get("back").isEmpty()) {
            event.forwardTo(QuizListingView.class);
            return;
        }
        quizId = parameters.get("quizId").get();
        redirect = parameters.get("back").get();
    }
}
