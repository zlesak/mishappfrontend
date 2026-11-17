package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizCreateView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizDetailView;
import lombok.extern.slf4j.Slf4j;

/**
 * A list item representing a quiz for listing purposes.
 */
@Slf4j
public class QuizListItem extends EntityRow {
    /**
     * Constructs a QuizListItem for the given quiz.
     *
     * @param quiz the quiz entity to represent
     * @param administrationView whether to show admin buttons
     */
    public QuizListItem(QuickQuizEntity quiz, boolean administrationView) {
        super(true, administrationView, VaadinIcon.LIGHTBULB);

        setRowTitle(quiz.getName());
        addCommonMetadata(quiz);

        if (quiz.getTimeLimit() != null) {
            String timeText = quiz.getTimeLimit() > 0
                    ? quiz.getTimeLimit() + " " + text(minutesKey(quiz.getTimeLimit()))
                    : text("quiz.timeLimit.none");
            addMetadata(text("quiz.timeLimit.label"), timeText);
        }

        // The chapter's name, not a fragment of its id: eight hex characters told the reader nothing
        // and looked like a defect. Quizzes stored before the name was recorded omit the entry.
        addMetadata(text("quiz.chapter.label"), quiz.getChapterName());

        setOpenButtonClickListener(e -> UI.getCurrent().navigate(QuizDetailView.class, new RouteParameters(new RouteParam("quizId", quiz.getId()))));
        setEditButtonClickListener(e -> {
            if (administrationView) {
                UI.getCurrent().navigate(QuizCreateView.class, new RouteParameters(new RouteParam("quizId", quiz.getId())));
            }
        });
        setDeleteButtonClickListener(e -> {
            if (administrationView) {
                ConfirmDialog dialog = ConfirmDialog.createDeleteConfirmation(
                    "quiz",
                    quiz.getName(),
                    () -> deleteQuiz(quiz.getId())
                );
                dialog.open();
            }
        });
    }

    private void deleteQuiz(String quizId) {
        UI sourceUi = UI.getCurrent();
        runBackendCallWithOverlay(() -> {
                    QuizService quizService = SpringContextUtils.getBean(QuizService.class);
                    return quizService.delete(quizId);
                }, deleted -> {
            if (deleted) {
                if (isUiInActive(sourceUi)) {
                    return;
                }
                new SuccessNotification(text("quiz.delete.success"));
                refreshParentListingFromBackend();
            } else {
                if (isUiInActive(sourceUi)) {
                    return;
                }
                new ErrorNotification(text("quiz.delete.failed"));
            }
        }, ex -> {
            log.error("Error deleting quiz: {}", ex.getMessage(), ex);
            if (isUiInActive(sourceUi)) {
                return;
            }
            new ErrorNotification(text("quiz.delete.error") + ": " + ex.getMessage());
        });
    }

    private boolean isUiInActive(UI ui) {
        return ui == null || ui.getSession() == null || !ui.isAttached() || ui.isClosing();
    }

    /**
     * Picks the Czech plural form for a number of minutes: one minuta, two to four minuty, otherwise
     * minut.
     *
     * @param minutes number of minutes, greater than zero.
     * @return translation key of the matching form.
     */
    private static String minutesKey(int minutes) {
        if (minutes == 1) {
            return "quiz.timeLimit.minutes.one";
        }
        return minutes < 5 ? "quiz.timeLimit.minutes.few" : "quiz.timeLimit.minutes.many";
    }
}
