package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.lists.AbstractListItem;
import cz.uhk.zlesak.threejslearningapp.components.lists.QuizResultListItem;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * QuizListingView Class - Displays a list of available quizzes to the user.
 * It fetches quiz data from the backend and displays it using QuizListItem component.
 */
@Slf4j
@Route("quizes-results")
@Scope("prototype")
@Tag("quizes-results-listing-view")
@PermitAll
public class QuizResultsListingView extends AbstractListingView<QuickQuizResult, QuizResultFilter, QuizValidationResult, QuizResultService> {
    private String quizId;

    /**
     * Constructor for QuizListingView.
     * It initializes the view with the necessary services using dependency injection.
     * Hides the filter via the super constructor.
     */
    @Autowired
    public QuizResultsListingView(QuizResultService quizResultService) {
        super(true, "page.title.quizListView", quizResultService, false);
    }

    /**
     * Creates a QuizListItem for the given QuickQuizEntity.
     *
     * @param quiz the quiz entity to create a list item for
     * @return a QuizListItem component representing the quiz
     */
    @Override
    protected AbstractListItem createListItem(QuickQuizResult quiz) {
        return new QuizResultListItem(quiz, administrationView, quizId);
    }

    /**
     * Creates a QuizFilter based on the provided search text.
     *
     * @param searchText the text to filter entities by
     * @return a QuizFilter object
     */
    @Override
    protected QuizResultFilter createFilter(String searchText) {
        return QuizResultFilter.builder()
                .Name(searchText)
                .quizId(quizId)
                .build();
    }

    /**
     * Handles actions to be performed after navigation to this view.
     * @param event the after navigation event
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        quizId = event.getRouteParameters().get("quizId").orElse("");
        filterParameters.getFilter().setQuizId(quizId);
        filter.setSearchFieldValue("");
        listEntities();
    }
}
