package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.lists.AbstractListItem;
import cz.uhk.zlesak.threejslearningapp.components.lists.QuizListItem;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizFilter;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
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
@Route("quizes")
@Scope("prototype")
@Tag("quizes-listing")
@PermitAll
public class QuizListingView extends AbstractListingView<QuickQuizEntity, QuizFilter, QuizEntity, QuizService> {

    /**
     * Constructor for QuizListingView.
     * It initializes the view with the necessary services using dependency injection.
     *
     * @param quizService service for handling quiz-related operations
     */
    @Autowired
    public QuizListingView(QuizService quizService) {
        super(true, "page.title.quizListView", quizService);
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
