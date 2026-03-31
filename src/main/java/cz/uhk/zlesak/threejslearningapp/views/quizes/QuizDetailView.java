package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.containers.QuizDetailContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.QuizResultsHistoryPanel;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractQuizView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * View for displaying quiz details before starting.
 */
@Slf4j
@Route("quiz/:quizId?")
@Scope("prototype")
@Tag("quiz-detail")
@PermitAll
public class QuizDetailView extends AbstractQuizView {
    private final QuizResultService quizResultService;

    /**
     * Constructor for QuizDetailView.
     *
     * @param quizResultService the quiz result service
     */
    @Autowired
    public QuizDetailView(QuizResultService quizResultService) {
        super("page.title.quizView");
        this.quizResultService = quizResultService;
        modelDiv.setVisible(false);
        modelDiv.getStyle().set("display", "none");
        modelSide.removeAll();
        modelSide.getStyle().set("overflow", "auto");
        setCompactSplitterPosition(58);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        runAsync(
                () -> new QuizDetailData(
                        service.readQuick(quizId),
                        quizResultService.readEntities(
                                new FilterParameters<>(
                                        PageRequest.of(0, 10, Sort.Direction.DESC, "Created"),
                                        QuizResultFilter.builder().Name("").quizId(quizId).build()
                                )
                        )
                ),
                this::displayQuizDetails,
                error -> {
                    log.error("Error loading quiz: {}", error.getMessage(), error);
                    new ErrorNotification(text("quiz.error.loading") + ": " + error.getMessage());
                }
        );
    }

    /**
     * Displays the quiz details in the view.
     */
    private void displayQuizDetails(QuizDetailData data) {
        entityContent.removeAll();
        entityContent.setAlignItems(FlexComponent.Alignment.STRETCH);
        entityContent.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        entityContent.addClassName(LumoUtility.Gap.MEDIUM);

        QuizDetailContainer detailContainer = new QuizDetailContainer(data.quiz());
        detailContainer.getStyle().set("margin", "0 auto");

        QuizResultsHistoryPanel historyPanel = new QuizResultsHistoryPanel(quizResultService, data.quiz().getId());
        historyPanel.renderInitialPage(data.resultsPage());
        modelSide.removeAll();
        modelSide.add(historyPanel);
        entityContent.add(detailContainer);
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

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
    }

    private record QuizDetailData(QuickQuizEntity quiz, PageResult<QuickQuizResult> resultsPage) {
    }
}
