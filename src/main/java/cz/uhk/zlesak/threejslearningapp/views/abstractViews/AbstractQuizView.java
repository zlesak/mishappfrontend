package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.components.containers.QuizResultContainer;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizDetailView;
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
    protected String redirect;

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

    /**
     * Displays the quiz result details using QuizResultComponent.
     * @param result Quiz validation result
     */
    protected void displayQuizResultDetails(QuizValidationResult result)
    {
        Button backButton = new Button(text("button.back"), new Icon(VaadinIcon.BACKWARDS));
        backButton.addClickListener(e -> {
            if (redirect != null && !redirect.isEmpty()) {
                getUI().ifPresent(ui -> ui.navigate(QuizDetailView.class,
                        new RouteParameters("quizId", redirect)));
            }else{
                getUI().ifPresent(ui -> ui.navigate(QuizDetailView.class,
                        new RouteParameters("quizId", quizId)));
            }
        });
        entityContent.removeAll();
        entityContent.add(backButton);
        String targetQuizId = redirect == null ? quizId : redirect;
        QuizEntity quiz = service.getQuizForStudent(targetQuizId);
        int possibleScore = quiz.getQuestions().stream().mapToInt(AbstractQuestionData::getPoints).sum();
        entityContent.add(new QuizResultContainer(result, quiz, possibleScore));
        splitLayout.setSplitterPosition(100);
    }
}
