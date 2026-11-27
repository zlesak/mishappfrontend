package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import cz.uhk.zlesak.threejslearningapp.common.DateFormater;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import cz.uhk.zlesak.threejslearningapp.services.conflict.ConflictChoice;
import cz.uhk.zlesak.threejslearningapp.services.conflict.ConflictField;
import lombok.Getter;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shown when a save is turned away because someone else saved the same chapter, quiz or model in
 * the meantime. Lays the user's version and the stored one side by side, field by field, and lets
 * the user decide which to keep for each.
 *
 * <p>Nothing is lost by opening it: closing it (button or Escape) returns to the editor with every
 * change still there. Saving the chosen combination is itself checked against the version shown
 * here, so a third save made while the dialog was open is caught again rather than overwritten.
 * Keeping "mine" everywhere is how the user knowingly overwrites the other change.
 */
public class ConflictResolutionDialog extends Dialog implements I18nAware {

    private final Map<String, RadioButtonGroup<ConflictChoice>> choiceGroups = new LinkedHashMap<>();

    @Getter
    private final Button saveButton;
    @Getter
    private final Button discardButton;
    @Getter
    private final Button backButton;

    /**
     * @param titleKey      text key of the heading, naming what was saved (chapter, quiz, model).
     * @param storedUpdated when the stored version was saved.
     * @param fields        the fields that differ.
     * @param onSave        receives the choice per field key when the user saves the combination.
     * @param onDiscard     throws the user's changes away and loads the stored version.
     */
    public ConflictResolutionDialog(String titleKey,
                                    Instant storedUpdated,
                                    List<ConflictField> fields,
                                    Consumer<Map<String, ConflictChoice>> onSave,
                                    Runnable onDiscard) {
        setHeaderTitle(text(titleKey));
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setWidth("min(64rem, 100vw)");
        setMaxHeight("100vh");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        // Stretched, each field takes the dialog's width, so the two versions sit side by side
        // even when their values are short; they only wrap below one another on narrow screens.
        content.setWidthFull();
        content.setAlignItems(FlexComponent.Alignment.STRETCH);

        content.add(new Paragraph(storedUpdated == null
                ? text("conflict.intro.unknownTime")
                : text("conflict.intro", DateFormater.formatDate(storedUpdated))));

        if (fields.isEmpty()) {
            content.add(new Paragraph(text("conflict.noDifferences")));
        } else {
            content.add(new Paragraph(text("conflict.instructions")));
            fields.forEach(field -> content.add(fieldSection(field)));
        }
        add(content);

        backButton = new Button(text("conflict.action.back"), event -> close());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        discardButton = new Button(text("conflict.action.discard"), event -> {
            close();
            onDiscard.run();
        });
        discardButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        saveButton = new Button(text("conflict.action.save"), event -> {
            close();
            onSave.accept(choices());
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footer = new HorizontalLayout(backButton, discardButton, saveButton);
        footer.setSpacing(true);
        footer.getStyle().set("flex-wrap", "wrap");
        getFooter().add(footer);

        // Start where the decision is made rather than on a button that would act on Enter.
        addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                choiceGroups.values().stream().findFirst()
                        .ifPresentOrElse(group -> group.getElement().executeJs(FOCUS_JS, CHOICE_SELECTOR),
                                () -> backButton.getElement().executeJs(FOCUS_JS, ""));
            }
        });
    }

    /**
     * The radio buttons of a choice; the checked one first. The group element itself cannot take
     * focus, so focusing it does nothing in the browser.
     */
    static final String CHOICE_SELECTOR = "vaadin-radio-button[checked], vaadin-radio-button";

    /**
     * Moves focus to {@code this} (selector empty) or to the first element under it matching the
     * selector. The dialog renders and traps focus on its own overlay after it opens, so a single
     * call right away is lost; this retries for about a second until the target has held focus.
     */
    static final String FOCUS_JS = """
            const host = this;
            const selector = $0;
            let tries = 0, held = 0;
            const step = () => {
              const target = selector ? host.querySelector(selector) : host;
              if (target && target.isConnected) {
                if (target.matches(':focus-within') || target.hasAttribute('focused')) {
                  if (++held >= 3) return;
                } else {
                  held = 0;
                  target.focus();
                }
              }
              if (++tries < 25) setTimeout(step, 50);
            };
            setTimeout(step, 0);
            """;

    /**
     * @return the current choice per field key; a field left untouched keeps the user's version.
     */
    public Map<String, ConflictChoice> choices() {
        Map<String, ConflictChoice> choices = new LinkedHashMap<>();
        choiceGroups.forEach((key, group) -> choices.put(key, group.getValue()));
        return choices;
    }

    /**
     * @param key field key.
     * @return the choice control of that field, or {@code null} when the field does not differ.
     */
    public RadioButtonGroup<ConflictChoice> choiceFor(String key) {
        return choiceGroups.get(key);
    }

    private Div fieldSection(ConflictField field) {
        H3 heading = new H3(field.label());
        heading.getStyle().set("margin", "0");

        FlexLayout versions = new FlexLayout(
                version(text("conflict.version.mine"), field.label(), field.mine(), field.mineImage()),
                version(text("conflict.version.theirs"), field.label(), field.theirs(), field.theirsImage()));
        versions.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        versions.setWidthFull();
        versions.getStyle().set("gap", "var(--lumo-space-m)");

        Map<ConflictChoice, String> labels = new EnumMap<>(ConflictChoice.class);
        labels.put(ConflictChoice.MINE, text("conflict.choice.mine"));
        labels.put(ConflictChoice.THEIRS, text("conflict.choice.theirs"));

        RadioButtonGroup<ConflictChoice> choice = new RadioButtonGroup<>(text("conflict.choice.label", field.label()));
        choice.setItems(ConflictChoice.MINE, ConflictChoice.THEIRS);
        choice.setItemLabelGenerator(labels::get);
        choice.setValue(ConflictChoice.MINE);
        choice.setRequired(true);
        choiceGroups.put(field.key(), choice);

        Div section = new Div(heading, versions, choice);
        section.setWidthFull();
        section.getElement().setAttribute("role", "group");
        section.getElement().setAttribute("aria-label", field.label());
        section.getStyle()
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-top", "var(--lumo-space-s)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        return section;
    }

    private Div version(String caption, String fieldLabel, String value, String imageUrl) {
        Span title = new Span(caption);
        title.getStyle().set("font-weight", "600");

        Div text = new Div();
        text.setText(value == null || value.isEmpty() ? text("conflict.empty") : value);
        // A long chapter scrolls inside its box; the region is focusable and named so a keyboard or
        // screen reader user can reach and read all of it.
        text.getElement().setAttribute("tabindex", "0");
        text.getElement().setAttribute("role", "region");
        text.getElement().setAttribute("aria-label", caption + ": " + fieldLabel);
        text.getStyle()
                .set("white-space", "pre-wrap")
                .set("overflow-wrap", "anywhere")
                .set("max-height", "16rem")
                .set("overflow", "auto")
                .set("padding", "var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Div column = new Div(title);
        if (imageUrl != null && !imageUrl.isBlank()) {
            Image image = new Image(imageUrl, text("conflict.image.alt", caption, fieldLabel));
            image.setMaxWidth("10rem");
            column.add(image);
        }
        column.add(text);
        column.getStyle()
                .set("flex", "1 1 20rem")
                .set("min-width", "0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-xs)");
        return column;
    }
}
