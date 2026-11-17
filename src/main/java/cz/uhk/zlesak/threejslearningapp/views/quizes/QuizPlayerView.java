package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.QuizPlayerComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractQuizView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
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
    private QuizPlayerComponent playerComponent;
    private QuizEntity loadedQuiz;
    private int loadedQuizPossibleScore;

    /**
     * Constructor for QuizPlayerView.
     */
    public QuizPlayerView() {
        super("page.title.quizView");
    }

    /**
     * Handles actions to be performed after navigation to this view.
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        runAsync(
                () -> service.getQuizForStudent(quizId),
                this::displayQuiz,
                error -> {
                    log.error("Error loading quiz: {}", error.getMessage(), error);
                    showErrorNotification(text("quiz.error.loading"), error);
                }
        );
    }

    /**
     * Displays the quiz using QuizPlayerComponent.
     * Shows timer in the top-right corner if the quiz has a time limit, ensuring it stays visible while scrolling through questions.
     * Questions are surrounded by a scroller to allow for better navigation, especially for quizzes with many questions or long content.
     *
     * @param quiz the quiz entity to be displayed
     */
    private void displayQuiz(QuizEntity quiz) {
        loadedQuiz = quiz;
        loadedQuizPossibleScore = quiz.getQuestions().stream().mapToInt(AbstractQuestionData::getPoints).sum();
        modelDiv.modelTextureAreaSelectContainer.setEnabled(false);
        playerComponent = new QuizPlayerComponent(quiz.getQuestions(), quiz.getTimeLimit(),
                quiz.getRemainingSeconds());
        playerComponent.setSubmitListener(this::submitQuiz);

        Scroller scroller = new Scroller(playerComponent, Scroller.ScrollDirection.VERTICAL);
        scroller.setSizeFull();

        // The header sits above the scroller rather than inside it, so the countdown and the progress
        // stay visible without ever covering the question. The old timer floated over the content with
        // position: sticky, which at 400 % zoom hides whichever element the user is filling in.
        Div header = playerComponent.getQuizHeader();
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Background.BASE);

        entityContent.add(header, scroller);
    }

    /**
     * Submits the quiz answers for validation and displays the results.
     */
    private void submitQuiz() {
        if (loadedQuiz == null) {
            showErrorNotification(text("quiz.error.submit"), "Kvíz není načtený.");
            return;
        }
        playerComponent.disable();
        runAsync(
                () -> {
                    try {
                        return service.validateAnswers(quizId, new ArrayList<>(playerComponent.getAnswers().values()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                this::showResult,
                error -> {
                    log.error("Error při odeslání odpovědí kvízu", error);
                    showErrorNotification(text("quiz.error.submit"), error);
                    playerComponent.enable();
                }
        );
    }

    /**
     * Shows the graded attempt at its own address.
     *
     * <p>The result used to be drawn in place, still at {@code /playQuiz/{id}}. A student could not
     * bookmark it or send anyone the link, and the browser's Back button returned them to a
     * half-finished quiz that could no longer be submitted. The {@code /quiz-result} route already
     * existed and the attempt listing already used it.
     *
     * <p>If the attempt came back without an id there is nothing to address, so it is drawn in place
     * rather than losing the result the student has just earned.
     *
     * @param result the graded attempt
     */
    private void showResult(QuizValidationResult result) {
        String resultId = result == null ? null : result.getId();
        if (resultId == null || resultId.isBlank()) {
            log.warn("Quiz result for quiz {} has no id, showing it in place", quizId);
            displayQuizResultDetails(result, loadedQuiz, loadedQuizPossibleScore);
            return;
        }

        skipBeforeLeaveDialog = true;
        getUI().ifPresent(ui -> ui.navigate(QuizResultView.class, new RouteParameters(
                new RouteParam("quizId", resultId),
                new RouteParam("back", quizId))));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        if (parameters.getParameterNames().isEmpty() || parameters.get("quizId").isEmpty()) {
            event.forwardTo(QuizListingView.class);
            return;
        }
        quizId = parameters.get("quizId").get();
    }
}
