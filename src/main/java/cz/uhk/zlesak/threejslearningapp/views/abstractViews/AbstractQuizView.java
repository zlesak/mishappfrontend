package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Tag;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

@Slf4j
@Tag("quiz-scaffold")
@Scope("prototype")
public abstract class AbstractQuizView extends AbstractEntityView<QuizService> {
    protected String quizId;
    public AbstractQuizView(String pageTitleKey) {
        this(pageTitleKey, true, SpringContextUtils.getBean(QuizService.class));
    }

    public AbstractQuizView(String pageTitleKey, boolean skipBeforeLeaveDialog, QuizService service) {
        super(pageTitleKey, skipBeforeLeaveDialog, service);
    }
}
