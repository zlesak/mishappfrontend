package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.containers.QuizDetailContainer;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
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
    private final QuizService quizService;

    @Autowired
    public QuizDetailView(QuizService quizService) {
        super("page.title.quizView");
        this.quizService = quizService;
    }

    @Override
    protected void afterNavigationActions() {
        try {
            QuickQuizEntity quiz = quizService.readQuick(quizId);
            displayQuizDetails(quiz);
        } catch (Exception e) {
            log.error("Error loading quiz: {}", e.getMessage(), e);
            new ErrorNotification(text("quiz.error.loading") + ": " + e.getMessage(), 5000);
        }
    }

    /**
     * Displays the quiz details in the view.
     * @param quiz Quiz entity to display
     */
    private void displayQuizDetails(QuickQuizEntity quiz) {
        entityContent.removeAll();
        entityContent.setAlignItems(FlexComponent.Alignment.CENTER);
        entityContent.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        QuizDetailContainer detailContainer = new QuizDetailContainer(quiz);

        entityContent.add(detailContainer);
    }

}
