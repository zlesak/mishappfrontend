package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.DateFormater;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * The quiz's parameters, as a description list.
 *
 * <p>It used to be a stack of {@code Div}s each holding two {@code Span}s pushed apart with
 * {@code justify-content: between} — a table to look at and nothing at all to a screen reader, which
 * read out five labels and five values with no way of telling which value belonged to which label. A
 * {@code <dl>} states that pairing in the markup.
 */
public class QuizDetailTableComponent extends DescriptionList implements I18nAware {

    /**
     * Creates a quiz detail table component.
     * @param quiz Quiz entity containing details to display
     */
    public QuizDetailTableComponent(QuickQuizEntity quiz) {
        super();
        addClassName("quiz-detail-table");
        getStyle().set("margin", "0").set("width", "100%");

        if (quiz.getDescription() != null && !quiz.getDescription().isBlank()) {
            addDetail(text("quiz.detail.description"), quiz.getDescription());
        }

        String timeLimitValue = quiz.getTimeLimit() != null && quiz.getTimeLimit() > 0
                ? quiz.getTimeLimit() + " " + text("quiz.detail.timeLimit.minutes")
                : text("quiz.detail.timeLimit.unlimited");
        addDetail(text("quiz.detail.timeLimit"), timeLimitValue);

        // The chapter's name, not its id. The id was shown verbatim, which told the reader nothing and
        // exposed an internal identifier; quizzes stored before the name was recorded say so instead.
        String chapterValue = quiz.getChapterName() != null && !quiz.getChapterName().isBlank()
                ? quiz.getChapterName()
                : text("quiz.detail.chapter.none");
        addDetail(text("quiz.detail.chapter"), chapterValue);

        if (quiz.getCreated() != null) {
            addDetail(text("quiz.detail.created"), DateFormater.formatDate(quiz.getCreated()));
        }

        if (quiz.getUpdated() != null) {
            addDetail(text("quiz.detail.updated"), DateFormater.formatDate(quiz.getUpdated()));
        }
    }

    /**
     * Appends one labelled parameter.
     *
     * @param label the parameter name
     * @param value the parameter value
     */
    private void addDetail(String label, String value) {
        Term term = new Term(label);
        term.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextColor.SECONDARY);

        Description description = new Description(value);
        description.addClassName(LumoUtility.TextColor.BODY);
        description.getStyle()
                .set("margin", "0 0 var(--lumo-space-s)")
                .set("padding-bottom", "var(--lumo-space-s)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        add(term, description);
    }
}
