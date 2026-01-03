package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.quiz.QuizResultComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
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
@Route("quiz-result/:quizId?/:back?")
@Scope("prototype")
@Tag("quiz-detail")
@PermitAll
public class QuizResultView extends AbstractQuizView {
    private final QuizResultService quizResultService;
    private String redirect = null;

    @Autowired
    public QuizResultView(QuizResultService quizResultService) {
        super("page.title.quizView");
        this.quizResultService = quizResultService;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        try {
            QuizValidationResult quizResult = quizResultService.read(quizId);
            displayQuizResultDetails(quizResult);
        } catch (Exception e) {
            log.error("Error loading quiz result: {}", e.getMessage(), e);
            new ErrorNotification(text("quiz.error.loading") + ": " + e.getMessage(), 5000);
        }
    }

    private void displayQuizResultDetails(QuizValidationResult result) {
        Button backButton = new Button(text("button.back"), new Icon(VaadinIcon.BACKWARDS));
        backButton.addClickListener(e -> {
            if (redirect != null && !redirect.isEmpty()) {
                getUI().ifPresent(ui -> ui.navigate(QuizDetailView.class,
                        new RouteParameters("quizId", redirect)));
            }
        });
        entityContent.removeAll();
        entityContent.add(backButton);
        entityContent.add(new QuizResultComponent(result));
        splitLayout.setSplitterPosition(100);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        if (parameters.getParameterNames().isEmpty() || parameters.get("quizId").isEmpty() || parameters.get("back").isEmpty()) {
            event.forwardTo(QuizListingView.class);
        }
        quizId = parameters.get("quizId").get();
        redirect = parameters.get("back").get();
    }
}
