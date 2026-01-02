package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;

/**
 * A list item representing a quiz for listigng purposes.
 */
public class QuizListItem extends AbstractListItem {
    /**
     * Constructs a QuizListItem for the given quiz.
     *
     * @param quiz the quiz entity to represent
     */
    public QuizListItem(QuickQuizEntity quiz, boolean administrationView) {
        super(true, administrationView);

        HorizontalLayout quizName = new HorizontalLayout();
        Span nameLabel = new Span(text("quiz.name.label") + ": ");
        Span name = new Span(quiz.getName());
        name.getStyle().set("font-weight", "600");
        quizName.add(nameLabel, name);
        details.add(quizName);


        setOpenButtonClickListener(e -> UI.getCurrent().navigate("quiz/" + quiz.getId()));
        setEditButtonClickListener(e -> UI.getCurrent().navigate("createQuiz/" + quiz.getId()));
    }
}

