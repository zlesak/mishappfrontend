package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.lists.AbstractListItem;
import cz.uhk.zlesak.threejslearningapp.components.lists.QuizListItem;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizFilter;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * QuizListingView Class - Displays a list of available quizzes to the user.
 * It fetches quiz data from the backend and displays it using QuizListItem component.
 */
@Slf4j
@Route("quizes")
@Scope("prototype")
@Tag("quizes-listing")
@PermitAll
public class QuizListingView extends AbstractListingView<QuickQuizEntity, QuizFilter> {
    private final QuizService quizService;

    /**
     * Constructor for QuizListingView.
     * It initializes the view with the necessary services using dependency injection.
     *
     * @param quizService service for handling quiz-related operations
     */
    @Autowired
    public QuizListingView(QuizService quizService) {
        super(true, "page.title.quizListView");
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new QuizFilter(""));
        this.quizService = quizService;
    }

    /**
     * Handles actions to be performed after navigation to this view.
     *
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        filterParameters = new FilterParameters<>(PageRequest.of(0, 6, Sort.Direction.ASC, "Name"), new QuizFilter(""));
        filter.setSearchFieldValue(filterParameters.getFilter().getSearchText());
        listEntities();
    }

    /**
     * Fetches a page of quizzes based on the provided filter parameters.
     *
     * @param params the filter parameters including pagination and filtering criteria
     * @return a PageResult containing the fetched quizzes and pagination info
     */
    @Override
    protected PageResult<QuickQuizEntity> fetchPage(FilterParameters<QuizFilter> params) {
        return quizService.readEntities(filterParameters);
    }

    /**
     * Creates a QuizListItem for the given QuickQuizEntity.
     *
     * @param quiz the quiz entity to create a list item for
     * @return a QuizListItem component representing the quiz
     */
    @Override
    protected AbstractListItem createListItem(QuickQuizEntity quiz) {
        return new QuizListItem(quiz);
    }

    /**
     * Creates a QuizFilter based on the provided search text.
     *
     * @param searchText the text to filter entities by
     * @return a QuizFilter object
     */
    @Override
    protected QuizFilter createFilter(String searchText) {
        return new QuizFilter(searchText);
    }
}
