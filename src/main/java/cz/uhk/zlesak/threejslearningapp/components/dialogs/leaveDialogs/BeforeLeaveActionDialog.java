package cz.uhk.zlesak.threejslearningapp.components.dialogs.leaveDialogs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.router.BeforeLeaveEvent;
import cz.uhk.zlesak.threejslearningapp.i18n.CustomI18NProvider;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;

import java.util.function.Consumer;
import java.util.Locale;

/**
 * BeforeLeaveActionDialog Class - Provides a dialog to confirm the user's intention to leave a page
 * when there are unsaved changes, ensuring that the user is aware of potential data loss.
 */
public class BeforeLeaveActionDialog {
    /**
     * Leave method - Displays a confirmation dialog when the user attempts to leave a page
     * @param event the event of leaving from the current page implementing BeforeLeaveEvent
     */
    public static void leave(BeforeLeaveEvent event) {
        leave(event, null);
    }

    /**
     * Leave method - Displays a confirmation dialog when the user attempts to leave a page
     *
     * @param event the BeforeLeaveEvent
     * @param onConfirm a consumer receiving the postponed ContinueNavigationAction; must call proceed() or cancel()
     */
    public static void leave(BeforeLeaveEvent event, Consumer<BeforeLeaveEvent.ContinueNavigationAction> onConfirm) {
        createAndOpenDialog(event, onConfirm);
    }

    private static void createAndOpenDialog(BeforeLeaveEvent event, Consumer<BeforeLeaveEvent.ContinueNavigationAction> onConfirm) {
        CustomI18NProvider i18n = SpringContextUtils.getBean(CustomI18NProvider.class);
        Locale locale = UI.getCurrent().getLocale();
        BeforeLeaveEvent.ContinueNavigationAction postponed = event.postpone();
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle(i18n.getTranslation("beforeLeaveActionDialog.leave.info.assurance", locale));
        confirmDialog.add(i18n.getTranslation("beforeLeaveActionDialog.leave.info.unsavedChanges", locale));

        Button leaveButton = new Button(i18n.getTranslation("leave", locale), e -> {
            confirmDialog.close();
            try {
                if (onConfirm != null) {
                    onConfirm.accept(postponed);
                } else {
                    postponed.proceed();
                }
            } catch (Exception ex) {
                postponed.proceed();
            }
        });
        Button stayButton = new Button(i18n.getTranslation("stay", locale), e -> {
            postponed.cancel();
            confirmDialog.close();
        });
        confirmDialog.getFooter().add(leaveButton, stayButton);
        confirmDialog.setCloseOnEsc(false);
        confirmDialog.setCloseOnOutsideClick(false);
        confirmDialog.open();
    }
}
