package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.tables.QuizDetailTable;
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
        setWidth("600px");
        setPadding(true);
        setSpacing(true);
        addClassName(LumoUtility.Background.CONTRAST_5);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Padding.LARGE);

        H2 title = new H2(quiz.getName());
        title.addClassName(LumoUtility.Margin.Bottom.MEDIUM);
        add(title);

        Div table = new QuizDetailTable(quiz);
        add(table);

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
