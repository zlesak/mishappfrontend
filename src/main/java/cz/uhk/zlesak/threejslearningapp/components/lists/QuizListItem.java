package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;

/**
 * A list item representing a quiz for listing purposes.
 */
public class QuizListItem extends AbstractListItem {
    /**
     * Constructs a QuizListItem for the given quiz.
     *
     * @param quiz the quiz entity to represent
     * @param administrationView whether to show admin buttons
     */
    public QuizListItem(QuickQuizEntity quiz, boolean administrationView) {
        super(true, administrationView, VaadinIcon.LIGHTBULB);

        titleSpan.setText(quiz.getName());

        if (quiz.getTimeLimit() != null && quiz.getTimeLimit() > 0) {
            HorizontalLayout timeLimitRow = new HorizontalLayout();
            timeLimitRow.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

            Icon clockIcon = VaadinIcon.CLOCK.create();
            clockIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span label = new Span("Časový limit:");
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            String timeText = quiz.getTimeLimit() + " " + (quiz.getTimeLimit() == 1 ? "minuta" : quiz.getTimeLimit() < 5 ? "minuty" : "minut");
            Span value = new Span(timeText);
            value.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.SEMIBOLD);

            timeLimitRow.add(clockIcon, label, value);
            details.add(timeLimitRow);
        }

        if (quiz.getChapterId() != null && !quiz.getChapterId().isBlank()) {
            HorizontalLayout chapterRow = new HorizontalLayout();
            chapterRow.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

            Icon bookIcon = VaadinIcon.BOOK.create();
            bookIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span label = new Span("Kapitola:");
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Span value = new Span(quiz.getChapterId().substring(0, Math.min(8, quiz.getChapterId().length())));
            value.addClassNames(LumoUtility.FontSize.SMALL);

            chapterRow.add(bookIcon, label, value);
            details.add(chapterRow);
        }

        setOpenButtonClickListener(e -> UI.getCurrent().navigate("quiz/" + quiz.getId()));
        setEditButtonClickListener(e -> UI.getCurrent().navigate("createQuiz/" + quiz.getId()));
    }
}

