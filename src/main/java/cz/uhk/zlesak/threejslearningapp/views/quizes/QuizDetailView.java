package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.containers.QuizDetailContainer;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractQuizView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * View for displaying quiz details before starting.
 */
@Slf4j
@Route("quiz/:quizId?")
@Scope("prototype")
@Tag("quiz-detail")
@PermitAll
public class QuizDetailView extends AbstractQuizView {

    /**
     * Constructor for QuizDetailView.
     *
     * @param quizResultService the quiz result service
     */
    @Autowired
    public QuizDetailView(QuizResultService quizResultService) {
        super("page.title.quizView");
        replaceModelWithQuizResultListing(quizResultService);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        runAsync(
                () -> service.readQuick(quizId),
                this::displayQuizDetails,
                error -> {
                    log.error("Error loading quiz: {}", error.getMessage(), error);
                    new ErrorNotification(text("quiz.error.loading") + ": " + error.getMessage());
                }
        );
    }

    /**
     * Displays the quiz details in the view.
     *
     * @param quiz Quiz entity to display
     */
    private void displayQuizDetails(QuickQuizEntity quiz) {
        entityContent.removeAll();
        entityContent.setAlignItems(FlexComponent.Alignment.CENTER);
        entityContent.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        QuizDetailContainer detailContainer = new QuizDetailContainer(quiz);

        entityContent.add(detailContainer);
    }

    /**
     * Replaces the 3D model area with a quiz result listing.
     * @param quizResultService the quiz result service
     */
    private void replaceModelWithQuizResultListing(QuizResultService quizResultService) {
        modelDiv.renderer.dispose(null);
        modelDiv.setHeight("0");
        modelDiv.setWidth("0");

        Div resultHistoryListingDiv = new Div(new QuizResultsListingView(quizResultService));
        resultHistoryListingDiv.setSizeFull();
        modelSide.addComponentAsFirst(resultHistoryListingDiv);
        modelSide.addClassNames(LumoUtility.Overflow.HIDDEN);
    }

    /**
     * Overridden beforeEnter function to check if the quizId parameter is present in the URL.
     * If not, it redirects the user to the QuizListingView.
     *
     * @param event before navigation event with event details
     */
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
