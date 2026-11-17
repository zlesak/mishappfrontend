package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.DateFormater;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One entity as a full-width row: a type icon, the name as an H2, a wrapping line of metadata and the
 * actions on the right.
 *
 * <p>Replaces the card grid this class grew out of. A card in a five-column grid is roughly 240 px
 * wide, which is why every value in it had to be truncated with an ellipsis and why the chapter card
 * needed a ResizeObserver to decide how many model names would fit. A row is as wide as the page, so
 * names simply fit; where they do not, they wrap onto a second line instead of being cut.
 *
 * <p>The name is an H2 rather than a styled span so that a screen reader user can move between
 * entries with the heading shortcut, and so the H1 from {@code PageHeader} has something under it.
 *
 * <p>The click-listener API is unchanged from the card it replaces, so the entity-specific subclasses
 * keep working as they did.
 */
public class EntityRow extends HorizontalLayout implements I18nAware {

    /** Wide enough for a comfortable target on touch, per WCAG 2.5.8. */
    private static final String MIN_ACTION_SIZE = "40px";

    protected final H2 titleHeading = new H2();
    protected final Div metadata = new Div();
    protected final HorizontalLayout actionsLayout = new HorizontalLayout();

    private final Icon typeIcon;
    private Component leadingVisual;
    private final Button editButton;
    private final Button deleteButton;
    private final Button selectButton;
    private final Button openButton;

    /**
     * Constructs the row.
     *
     * @param listView whether the row is shown in a listing (open) or in a picker dialog (select)
     * @param administrationView whether to show the edit and delete controls
     * @param icon the icon standing for the entity type
     */
    public EntityRow(boolean listView, boolean administrationView, VaadinIcon icon) {
        addClassNames(
                "entity-row",
                LumoUtility.Background.BASE,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.MEDIUM
        );
        setWidthFull();
        setPadding(true);
        setSpacing(true);
        setWrap(true);
        setAlignItems(Alignment.CENTER);
        getStyle()
                .set("min-height", "68px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("box-sizing", "border-box");

        typeIcon = icon.create();
        typeIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.SECONDARY);
        // Decorative: the row already says in words what it is.
        typeIcon.getElement().setAttribute("aria-hidden", "true");
        typeIcon.getStyle().set("flex", "0 0 auto");
        leadingVisual = typeIcon;

        titleHeading.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
        titleHeading.getStyle().set("margin", "0");

        metadata.addClassNames("entity-row-meta", LumoUtility.FontSize.SMALL);
        metadata.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-xs) var(--lumo-space-m)");

        Div body = new Div(titleHeading, metadata);
        body.addClassName("entity-row-body");
        body.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-xs)")
                .set("flex", "1 1 16rem")
                .set("min-width", "0");

        openButton = getOpenButton(listView);
        selectButton = getSelectButton(listView);
        editButton = getEditButton(administrationView);
        deleteButton = getDeleteButton(administrationView);

        actionsLayout.addClassNames("entity-row-actions", LumoUtility.Gap.SMALL);
        actionsLayout.setPadding(false);
        actionsLayout.setSpacing(true);
        actionsLayout.setWrap(true);
        actionsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        actionsLayout.getStyle().set("flex", "0 0 auto");
        actionsLayout.add(deleteButton, editButton, selectButton, openButton);

        add(typeIcon, body, actionsLayout);
        expand(body);
    }

    /**
     * Sets the row title.
     *
     * @param title the entity name
     */
    protected void setRowTitle(String title) {
        titleHeading.setText(title == null ? "" : title);
    }

    /**
     * Adds one labelled fact to the metadata line, for example {@code Autor: Jan Novák}. The label is
     * always rendered: a bare date with the label hidden on narrow screens leaves the reader guessing
     * which date it is.
     *
     * @param label the name of the fact, without a colon
     * @param value the value, ignored when blank
     */
    protected void addMetadata(String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Span item = new Span();
        Span labelSpan = new Span(label + ": ");
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        item.add(labelSpan, new Span(value));
        metadata.add(item);
    }

    /**
     * Adds a prepared component to the metadata line, for entries that are more than a label and a
     * string.
     *
     * @param items the components to append
     */
    protected void addMetadata(Component... items) {
        metadata.add(items);
    }

    /**
     * Adds the three facts every entity has: who made it, when, and when it last changed.
     *
     * <p>Only chapters used to show them. A model or a quiz gave no clue whose it was or how old it
     * was, which is precisely what a teacher looking through someone else's material needs to know.
     *
     * @param entity the entity being listed
     */
    protected void addCommonMetadata(AbstractEntity entity) {
        if (entity == null) {
            return;
        }
        // The author's name, never their id: the id is a Keycloak subject, which means nothing to a
        // reader and gives away who else uses the system.
        addMetadata(text("chapter.creator"), entity.displayCreator());
        addMetadata(text("chapter.creationDate"), entity.getCreated() == null
                ? null
                : DateFormater.formatDate(entity.getCreated()));
        addMetadata(text("chapter.lastModified"), entity.getUpdated() == null
                ? null
                : DateFormater.formatDate(entity.getUpdated()));
    }

    /**
     * Replaces the type icon with a richer visual, such as a model thumbnail.
     *
     * @param visual the component to show at the start of the row
     */
    protected void setLeadingVisual(Component visual) {
        if (visual == null) {
            return;
        }
        replace(leadingVisual, visual);
        leadingVisual = visual;
    }

    private Button getSelectButton(boolean listView) {
        Button selectButton = new Button(text("button.select"));
        selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        selectButton.setVisible(!listView);
        styleActionButton(selectButton);
        return selectButton;
    }

    private Button getOpenButton(boolean listView) {
        Button button = listView
                ? new Button(text("button.open"))
                : new Button(text("button.open"), VaadinIcon.EXTERNAL_BROWSER.create());
        button.addThemeVariants(listView ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_CONTRAST);
        styleActionButton(button);
        return button;
    }

    private Button getEditButton(boolean administrationView) {
        Button editButton = new Button(text("button.edit"));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.setVisible(administrationView);
        styleActionButton(editButton);
        // A tertiary button is a word with no boundary of its own; an outline gives it one.
        editButton.getStyle().set("border", "1px solid var(--lumo-contrast-30pct)");
        return editButton;
    }

    private Button getDeleteButton(boolean administrationView) {
        Button deleteButton = new Button(text("button.delete"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.setVisible(administrationView);
        styleActionButton(deleteButton);
        // The destructive action must not be told apart by the colour of its text alone.
        deleteButton.getStyle().set("border", "1px solid var(--lumo-error-color)");
        return deleteButton;
    }

    private void styleActionButton(Button button) {
        button.addClassNames(LumoUtility.FontSize.SMALL);
        button.getStyle()
                .set("min-height", MIN_ACTION_SIZE)
                .set("flex", "0 0 auto");
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

    protected void refreshParentListingFromBackend() {
        var parent = getParent();
        while (parent.isPresent()) {
            if (parent.get() instanceof AbstractListingView<?, ?, ?, ?> listingView) {
                listingView.listEntities();
                return;
            }
            parent = parent.get().getParent();
        }
    }
}
