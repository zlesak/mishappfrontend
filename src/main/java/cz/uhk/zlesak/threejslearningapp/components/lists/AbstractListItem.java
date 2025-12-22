package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * AbstractListItem Class - A base class for list items in the application.
 * It extends HorizontalLayout and implements I18nAware for internationalization support.
 * This class provides a common layout for list items with details and action buttons.
 */
public class AbstractListItem extends HorizontalLayout implements I18nAware {
    VerticalLayout details = new VerticalLayout();
    private final Button selectButton;
    private final Button openButton;

    /**
     * Constructor for AbstractListItem.
     * Initializes the layout and components for list items.
     * @param listView indicates whether the item is displayed in list view mode or select mode
     */
    public AbstractListItem(boolean listView) {
        setWidthFull();
        setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        setAlignItems(HorizontalLayout.Alignment.CENTER);
        getStyle().set("border", "1px solid #ccc");
        getStyle().set("border-radius", "8px");

        openButton = getOpenButton(listView);
        selectButton = getSeletButton(listView);

        add(details, openButton, selectButton);
    }

    /**
     * Creates and returns the select button.
     * @param listView indicates whether the item is displayed in list view mode or select mode
     * @return the select button
     */
    private Button getSeletButton(boolean listView) {
        Button selectButton = new Button(text("button.select"));
        selectButton.getStyle().set("margin", "12px").set("padding", "8px 24px");
        if (!listView){
            selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
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
        button.getStyle().set("margin", "12px").set("padding", "8px 24px");
        if (listView){
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        return button;
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
}
