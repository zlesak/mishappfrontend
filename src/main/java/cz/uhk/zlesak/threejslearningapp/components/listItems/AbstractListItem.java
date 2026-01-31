package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * AbstractListItem Class - A base class for list items in the application.
 * It extends VerticalLayout and implements I18nAware for internationalization support.
 * This class provides a common card-based layout for list items with details and action buttons.
 */
public class AbstractListItem extends VerticalLayout implements I18nAware {
    protected VerticalLayout details = new VerticalLayout();
    protected HorizontalLayout headerLayout = new HorizontalLayout();
    protected HorizontalLayout actionsLayout = new HorizontalLayout();
    protected Span titleSpan = new Span();
    private final Button editButton;
    private final Button deleteButton;
    private final Button selectButton;
    private final Button openButton;

    /**
     * Constructor for AbstractListItem.
     * Initializes the layout and components for list items.
     * @param listView indicates whether the item is displayed in list view mode or select mode
     * @param icon the icon to display in the header
     */
    public AbstractListItem(boolean listView, boolean administrationView, VaadinIcon icon) {
        addClassNames(
            LumoUtility.Background.CONTRAST_5,
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.Border.ALL,
            LumoUtility.BorderColor.CONTRAST_10
        );
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        Icon headerIcon = icon.create();
        headerIcon.addClassNames(LumoUtility.IconSize.MEDIUM);

        titleSpan.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.FontWeight.SEMIBOLD
        );

        headerLayout.addClassNames(
            LumoUtility.Padding.MEDIUM,
            LumoUtility.Gap.SMALL,
            LumoUtility.AlignItems.CENTER,
            LumoUtility.Background.BASE
        );
        headerLayout.setWidthFull();
        headerLayout.add(headerIcon, titleSpan);

        details.addClassNames(
            LumoUtility.Padding.MEDIUM,
            LumoUtility.Gap.SMALL
        );
        details.setPadding(true);
        details.setSpacing(true);

        openButton = getOpenButton(listView);
        selectButton = getSeletButton(listView);
        editButton = getEditButton(administrationView);
        deleteButton = getDeleteButton(administrationView);

        actionsLayout.addClassNames(
            LumoUtility.Padding.MEDIUM,
            LumoUtility.Gap.SMALL,
            LumoUtility.JustifyContent.END,
            LumoUtility.Background.CONTRAST_5,
            LumoUtility.BorderColor.CONTRAST_10
        );
        actionsLayout.setWidthFull();
        actionsLayout.add(deleteButton, editButton, selectButton, openButton);

        add(headerLayout, details, actionsLayout);
    }

    /**
     * Creates and returns the select button.
     * @param listView indicates whether the item is displayed in list view mode or select mode
     * @return the select button
     */
    private Button getSeletButton(boolean listView) {
        Button selectButton = new Button(text("button.select"));
        selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        selectButton.setVisible(!listView);
        return selectButton;
    }

    /**
     * Creates and returns the open button.
     * @param listView indicates whether the item is displayed in list view mode or select mode
     * @return the open button
     */
    private Button getOpenButton(boolean listView) {
        Button button = new Button(text("button.open"));
        if (listView) {
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        }
        return button;
    }

    private Button getEditButton(boolean administrationView) {
        Button editButton = new Button(text("button.edit"));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.setVisible(administrationView);
        return editButton;
    }

    private Button getDeleteButton(boolean administrationView) {
        Button deleteButton = new Button(text("button.delete"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.setVisible(administrationView);
        return deleteButton;
    }

    /**
     * Sets the click listener for the select button.
     * @param listener the click event listener
     */
    public void setSelectButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        selectButton.addClickListener(listener);
    }

    /**
     * Sets the click listener for the open button.
     * @param listener the click event listener
     */
    public void setOpenButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        openButton.addClickListener(listener);
    }

    public void setEditButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        editButton.addClickListener(listener);
    }

    /**
     * Sets the click listener for the delete button.
     * @param listener the click event listener
     */
    public void setDeleteButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        deleteButton.addClickListener(listener);
    }
}
