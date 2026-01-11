package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Tag;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

/**
 * AbstractQuizView Class - Serves as a base view for quiz-related pages, providing common functionality
 * and structure for managing quizzes within the application.
 */
@Slf4j
@Tag("quiz-scaffold")
@Scope("prototype")
public abstract class AbstractQuizView extends AbstractEntityView<QuizService> {
    protected String quizId;

    /**
     * Constructor for AbstractQuizView.
     *
     * @param pageTitleKey the key for the page title
     */
    public AbstractQuizView(String pageTitleKey) {
        this(pageTitleKey, true, SpringContextUtils.getBean(QuizService.class));
    }

    /**
     * Constructor for AbstractQuizView with skipBeforeLeaveDialog option.
     *
     * @param pageTitleKey          the key for the page title
     * @param skipBeforeLeaveDialog flag to skip before leave dialog
     * @param service               the quiz service
     */
    public AbstractQuizView(String pageTitleKey, boolean skipBeforeLeaveDialog, QuizService service) {
        super(pageTitleKey, skipBeforeLeaveDialog, service);
    }
}
