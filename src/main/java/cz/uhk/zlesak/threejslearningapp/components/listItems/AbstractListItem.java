package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
     *
     * @param listView indicates whether the item is displayed in list view mode or select mode
     * @param icon     the icon to display in the header
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

        titleSpan.setWidthFull();
        applyEllipsis(titleSpan);

        headerLayout.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Background.BASE
        );

        headerLayout.setWidthFull();
        headerLayout.add(headerIcon, titleSpan);
        headerLayout.expand(titleSpan);

        details.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );
        details.setPadding(true);
        details.setSpacing(true);

        openButton = getOpenButton(listView);
        selectButton = getSelectButton(listView);
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

    protected void applyEllipsis(Span span) {
        span.getStyle()
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("min-width", "0");
    }

    private Button getSelectButton(boolean listView) {
        Button selectButton = new Button(text("button.select"));
        selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        selectButton.setVisible(!listView);
        return selectButton;
    }

    /**
     * Creates and returns the open button.
     *
     * @param listView indicates whether the item is displayed in list view mode or select mode
     * @return the open button
     */
    private Button getOpenButton(boolean listView) {
        Button button = new Button(text("button.open"));

        if(!listView) {
            button = new Button(text("button.open"), VaadinIcon.EXTERNAL_BROWSER.create());
        }
        button.addThemeVariants(listView
                ? ButtonVariant.LUMO_PRIMARY
                : ButtonVariant.LUMO_CONTRAST);
        return button;
    }

    /**
     * Creates and returns the edit button.
     *
     * @param administrationView indicates whether the item is displayed in administration view mode, which determines the visibility of the edit button
     * @return the edit button
     */
    private Button getEditButton(boolean administrationView) {
        Button editButton = new Button(text("button.edit"));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.setVisible(administrationView);
        return editButton;
    }

    /**
     * Creates and returns the delete button.
     *
     * @param administrationView indicates whether the item is displayed in administration view mode, which determines the visibility of the delete button
     * @return the delete button
     */
    private Button getDeleteButton(boolean administrationView) {
        Button deleteButton = new Button(text("button.delete"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.setVisible(administrationView);
        return deleteButton;
    }

    /**
     * Sets the click listener for the select button.
     *
     * @param listener the click event listener
     */
    public void setSelectButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        selectButton.addClickListener(listener);
    }

    /**
     * Sets the click listener for the open button.
     *
     * @param listener the click event listener
     */
    public void setOpenButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        openButton.addClickListener(listener);
    }

    /**
     * Sets the click listener for the edit button.
     *
     * @param listener the click event listener
     */
    public void setEditButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        editButton.addClickListener(listener);
    }

    /**
     * Sets the click listener for the delete button.
     *
     * @param listener the click event listener
     */
    public void setDeleteButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        deleteButton.addClickListener(listener);
    }

    protected <T> void runBackendCallWithOverlay(Supplier<T> supplier, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            onError.accept(new IllegalStateException("UI is not available"));
            return;
        }

        AbstractView<?> activeView = AbstractView.findCurrentAbstractView(ui);
        if (activeView != null) {
            activeView.executeAsyncWithOverlay(supplier, onSuccess, onError);
            return;
        }

        Executor ioExecutor = SpringContextUtils.getBean(Executor.class);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return supplier.get();
                    } catch (Throwable t) {
                        throw new CompletionException(t);
                    }
                }, ioExecutor)
                .whenComplete((result, error) -> {
                    if (ui.isClosing()) {
                        return;
                    }
                    ui.access(() -> {
                        if (error != null) {
                            Throwable cause = error instanceof CompletionException && error.getCause() != null
                                    ? error.getCause()
                                    : error;
                            onError.accept(cause);
                            return;
                        }
                        onSuccess.accept(result);
                    });
                });
    }
}
