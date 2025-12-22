package cz.uhk.zlesak.threejslearningapp.components.tables;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.DateFormater;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Table component displaying detailed information about a quiz.
 */
public class QuizDetailTable extends Div implements I18nAware {

    /**
     * Creates a quiz detail table component.
     * @param quiz Quiz entity containing details to display
     */
    public QuizDetailTable(QuickQuizEntity quiz) {
        super();
        addClassName(LumoUtility.Display.FLEX);
        addClassName(LumoUtility.FlexDirection.COLUMN);
        addClassName(LumoUtility.Gap.SMALL);
        setWidthFull();

        if (quiz.getDescription() != null && !quiz.getDescription().isBlank()) {
            add(createDetailRow(text("quiz.detail.description"), quiz.getDescription()));
        }

        String timeLimitValue = quiz.getTimeLimit() != null && quiz.getTimeLimit() > 0
                ? quiz.getTimeLimit() + " " + text("quiz.detail.timeLimit.minutes")
                : text("quiz.detail.timeLimit.unlimited");
        add(createDetailRow(text("quiz.detail.timeLimit"), timeLimitValue));

        String chapterValue = quiz.getChapterId() != null && !quiz.getChapterId().isBlank()
                ? quiz.getChapterId()
                : text("quiz.detail.chapter.none");
        add(createDetailRow(text("quiz.detail.chapter"), chapterValue));

        if (quiz.getCreated() != null) {
            add(createDetailRow(
                    text("quiz.detail.created"),
                    DateFormater.formatDate(quiz.getCreated())
            ));
        }

        if (quiz.getUpdated() != null) {
            add(createDetailRow(
                    text("quiz.detail.updated"),
                    DateFormater.formatDate(quiz.getUpdated())
            ));
        }
    }

    /**
     * Creates a single row in the detail table with a label and value.
     * @param label field label
     * @param value field value
     * @return Div representing the detail row
     */
    private Div createDetailRow(String label, String value) {
        Div row = new Div();
        row.addClassName(LumoUtility.Display.FLEX);
        row.addClassName(LumoUtility.JustifyContent.BETWEEN);
        row.addClassName(LumoUtility.Padding.Vertical.SMALL);
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.TextColor.BODY);

        row.add(labelSpan, valueSpan);
        return row;
    }
}
