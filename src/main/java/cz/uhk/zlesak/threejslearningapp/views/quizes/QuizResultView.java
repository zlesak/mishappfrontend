package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
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
                () -> quizResultService.read(quizId),
                this::displayQuizResultDetails,
                error -> {
                    log.error("Error loading quiz result: {}", error.getMessage(), error);
                    showErrorNotification(text("quiz.error.loading"), error);
                }
        );
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
