package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.QuizDetailTableComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizPlayerView;

/**
 * Container component displaying quiz details and a start button for beginning the quiz.
 */
public class QuizDetailContainer extends VerticalLayout implements I18nAware {

    /**
     * Creates a quiz detail container with information about the quiz and a start button.
     * @param quiz Quiz entity containing details to display
     */
    public QuizDetailContainer(QuickQuizEntity quiz) {
        super();
        setWidthFull();
        setMaxWidth("600px");
        setPadding(true);
        setSpacing(true);
        addClassName("quiz-detail-container");
        addClassName(LumoUtility.Background.CONTRAST_5);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Padding.LARGE);

        // No heading of its own: the route's PageHeader carries the quiz name as the page's H1, and a
        // second copy of the same name right below it is noise for anyone reading by heading.
        add(new QuizDetailTableComponent(quiz));

        Button startButton = new Button(text("quiz.detail.startButton"));
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        startButton.addClassName(LumoUtility.Margin.Top.MEDIUM);
        startButton.setWidthFull();
        startButton.addClickListener(e ->
                UI.getCurrent().navigate(QuizPlayerView.class,
                    new RouteParameters("quizId", quiz.getId()))
        );
        add(startButton);
    }
}
