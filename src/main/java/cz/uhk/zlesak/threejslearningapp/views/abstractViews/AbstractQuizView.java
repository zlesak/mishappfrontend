package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizListingView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

@Slf4j
@Tag("quiz-scaffold")
@Scope("prototype")
public abstract class AbstractQuizView extends AbstractEntityView {
    protected String quizId;
    public AbstractQuizView(String pageTitleKey) {
        this(pageTitleKey, true);
    }

    public AbstractQuizView(String pageTitleKey, boolean skipBeforeLeaveDialog) {
        super(pageTitleKey, skipBeforeLeaveDialog);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        if (parameters.getParameterNames().isEmpty() || parameters.get("quizId").isEmpty()) {
            event.forwardTo(QuizListingView.class);
        }
        quizId = parameters.get("quizId").get();
    }
}
