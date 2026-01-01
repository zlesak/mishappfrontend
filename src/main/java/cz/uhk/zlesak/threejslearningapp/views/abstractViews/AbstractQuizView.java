package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Tag;
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
}
