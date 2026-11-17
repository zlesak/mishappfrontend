package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * What to show when a picker dialog has nothing to pick.
 *
 * <p>An empty dialog with no explanation is a dead end: on a fresh installation the model picker
 * showed nothing at all, and the user had to guess that a model must first be uploaded from somewhere
 * else entirely. This says so, and offers the route.
 */
public class EmptyListingGuidance extends VerticalLayout implements I18nAware {

    /**
     * @param explanationKey i18n key of the sentence explaining why the list is empty
     * @param actionKey i18n key of the button label
     * @param route the route the button leads to
     */
    public EmptyListingGuidance(String explanationKey, String actionKey, String route) {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);

        Paragraph explanation = new Paragraph(text(explanationKey));
        explanation.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.TextAlignment.CENTER);
        explanation.getStyle().set("max-width", "34em");

        Button action = new Button(text(actionKey), VaadinIcon.UPLOAD.create());
        action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        action.addClickListener(e -> UI.getCurrent().navigate(route));

        add(explanation, action);
    }

    /**
     * @return guidance for an empty model picker.
     */
    public static Component forModels() {
        return new EmptyListingGuidance("dialog.empty.models", "dialog.empty.models.action", "createModel");
    }

    /**
     * @return guidance for an empty chapter picker.
     */
    public static Component forChapters() {
        return new EmptyListingGuidance("dialog.empty.chapters", "dialog.empty.chapters.action", "createChapter");
    }
}
