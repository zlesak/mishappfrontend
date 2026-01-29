package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.i18n.CustomI18NProvider;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

import java.util.Locale;

/**
 * ConfirmDialog - A reusable confirmation dialog component.
 * Displays a confirmation message with confirm and cancel buttons.
 */
public class ConfirmDialog extends Dialog implements I18nAware {
    
    /**
     * Creates a confirmation dialog.
     *
     * @param title the dialog title
     * @param message the confirmation message
     * @param confirmText the text for the confirm button
     * @param onConfirm callback to execute when confirmed
     * @param isDangerous if true, uses danger styling for confirm button
     */
    public ConfirmDialog(String title, String message, String confirmText, Runnable onConfirm, boolean isDangerous) {
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        
        Icon icon = isDangerous ? VaadinIcon.WARNING.create() : VaadinIcon.QUESTION_CIRCLE.create();
        icon.setSize("48px");
        icon.getStyle().set("color", isDangerous ? "var(--lumo-error-color)" : "var(--lumo-primary-color)");
        
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);
        
        H2 titleHeader = new H2(title);
        titleHeader.getStyle().set("margin", "0");
        
        headerLayout.add(icon, titleHeader);
        
        Paragraph messageText = new Paragraph(message);
        messageText.getStyle().set("margin-top", "var(--lumo-space-m)");
        
        Button confirmButton = new Button(confirmText);
        if (isDangerous) {
            confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        } else {
            confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        confirmButton.addClickListener(e -> {
            onConfirm.run();
            close();
        });
        
        Button cancelButton = new Button(text("button.cancel"));
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.add(cancelButton, confirmButton);
        
        layout.add(headerLayout, messageText, buttonLayout);
        add(layout);
    }
    
    /**
     * Creates a delete confirmation dialog with danger styling.
     *
     * @param entityName the name of the entity being deleted
     * @param onConfirm callback to execute when deletion is confirmed
     * @return a configured ConfirmDialog instance
     * @deprecated Use {@link #createDeleteConfirmation(String, String, Runnable)} instead
     */
    @Deprecated
    public static ConfirmDialog createDeleteConfirmation(String entityName, Runnable onConfirm) {
        CustomI18NProvider i18n = SpringContextUtils.getBean(CustomI18NProvider.class);
        Locale locale = UI.getCurrent() != null ? UI.getCurrent().getLocale() : Locale.getDefault();
        
        String title = i18n.getTranslation("dialog.delete.title", locale);
        String message = i18n.getTranslation("dialog.delete.message", locale, entityName);
        String confirmText = i18n.getTranslation("button.delete", locale);
        return new ConfirmDialog(title, message, confirmText, onConfirm, true);
    }

    /**
     * Creates a delete confirmation dialog with entity-specific texts.
     *
     * @param entityType the type of entity (e.g., "chapter", "model", "quiz")
     * @param entityName the name of the specific entity being deleted
     * @param onConfirm callback to execute when deletion is confirmed
     * @return a configured ConfirmDialog instance
     */
    public static ConfirmDialog createDeleteConfirmation(String entityType, String entityName, Runnable onConfirm) {
        CustomI18NProvider i18n = SpringContextUtils.getBean(CustomI18NProvider.class);
        Locale locale = UI.getCurrent() != null ? UI.getCurrent().getLocale() : Locale.getDefault();
        
        String title = i18n.getTranslation("dialog.delete." + entityType + ".title", locale);
        String message = i18n.getTranslation("dialog.delete." + entityType + ".message", locale, entityName);
        String confirmText = i18n.getTranslation("dialog.delete." + entityType + ".confirm", locale);
        return new ConfirmDialog(title, message, confirmText, onConfirm, true);
    }
}
