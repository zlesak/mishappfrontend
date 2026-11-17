package cz.uhk.zlesak.threejslearningapp.components.inputs.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import cz.uhk.zlesak.threejslearningapp.common.ObservableMap;
import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondaryMain;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GenericSelect is an abstract class extending Vaadin's Select component.
 * It is designed to handle items that implement the HasPrimarySecondary interface.
 * The class supports dynamic item display based on primary and secondary context,
 * and fires custom events on value changes.
 *
 * @param <T> the type of items in the select, extending HasPrimarySecondary
 * @param <E> the type of component event for item addition
 */
@Slf4j
@Scope("prototype")
public abstract class GenericSelect<T extends HasPrimarySecondaryMain, E extends ComponentEvent<UI>> extends Select<T> implements I18nAware {
    @Setter
    protected String questionId;
    protected ObservableMap<String, T> items;
    private final ItemLabelGenerator<T> itemLabelGenerator;

    /**
     * Constructor for GenericSelect.
     *
     * @param label               the label for the select component
     * @param itemLabelGenerator  the generator for item labels
     * @param itemType            the class type of the items
     * @param allowEmptySelection flag to allow empty selection
     */
    /**
     * Renders one texture area as a colour swatch followed by its name.
     *
     * <p>The area's colour used to be applied to the text itself, which made half the list unreadable:
     * a pale yellow area name on a white background is not a colour scheme, it is a coincidence. The
     * name now uses the ordinary text colour and the colour is shown beside it as a swatch, where being
     * pale costs nothing.
     *
     * @param area the texture area
     * @param areaName the name to show
     * @return the rendered row
     */
    private static Span renderTextureArea(TextureAreaForSelect area, String areaName) {
        Span row = new Span();
        row.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)");

        String color = area == null ? null : area.hexColor();
        if (color != null && !color.isBlank()) {
            Span swatch = new Span();
            swatch.getStyle()
                    .set("width", "1em")
                    .set("height", "1em")
                    .set("flex", "0 0 auto")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("border", "1px solid var(--lumo-contrast-30pct)")
                    .set("background-color", color);
            // Decorative: the name beside it is what identifies the area, and a colour is not
            // something a screen reader can convey anyway.
            swatch.getElement().setAttribute("aria-hidden", "true");
            row.add(swatch);
        }

        row.add(new Span(areaName));
        return row;
    }

    public GenericSelect(String label, ItemLabelGenerator<T> itemLabelGenerator, Class<T> itemType, boolean allowEmptySelection) {
        super();
        this.itemLabelGenerator = itemLabelGenerator;

        // Both, on purpose. The empty-selection caption is the "nothing chosen yet" entry inside the
        // list; it is not the field's accessible name, so on its own it left every one of these selects
        // announced as an unnamed combo box once a value had been picked.
        setEmptySelectionCaption(text(label));
        setLabel(text(label));
        setWidthFull();
        setRenderer(new TextRenderer<>(itemLabelGenerator));
        if (itemType == TextureAreaForSelect.class) {
            setRenderer(new ComponentRenderer<>(item -> renderTextureArea((TextureAreaForSelect) item,
                    itemLabelGenerator.apply(item))));
        }
        items = new ObservableMap<>((value, fromClient) -> {
            if (fromClient) {
                showRelevantItemsBasedOnContext(value != null ? value.primary() : null, value != null ? value.secondary() : null);
            }
        }
        );
        addValueChangeListener(e -> {
                    getElement().setProperty("title", e.getValue() != null ? itemLabelGenerator.apply(e.getValue()) : "");
                    if (e.isFromClient()) {
                        ComponentUtil.fireEvent(UI.getCurrent(), createChangeEvent(e));
                    }
                }
        );
        setWidthFull();
        setEnabled(false);
        setEmptySelectionAllowed(allowEmptySelection);
    }

    /**
     * Creates a custom change event based on the value change event.
     *
     * @param event the value change event
     * @return the created component event
     */
    abstract protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<T> event);

    /**
     * Handles item addition ingoing change event action.
     *
     * @param fileType the item to be added
     */
    abstract public void handleItemAdditionIngoingChangeEventAction(E fileType);

    /**
     * Handles item removal ingoing change event action.
     *
     * @param id         the ID of the item to be removed
     * @param fromClient flag indicating if the change is from the client
     */
    public void handleItemRemoveIngoingChangeEventAction(String id, boolean fromClient) {
        items.keySet().removeIf(k -> k.contains(id));
        items.notifyChange(null, fromClient);
    }

    /**
     * Shows relevant items based on the provided primary and secondary context.
     *
     * @param primary   primary context
     * @param secondary secondary context
     */
    public void showRelevantItemsBasedOnContext(String primary, String secondary) {
        LinkedHashMap<String, T> itemsToShow = new LinkedHashMap<>();
        if (primary != null) {
            for (Map.Entry<String, T> entry : items.entrySet()) {
                if (entry.getKey().contains(primary)) {
                    itemsToShow.put(entry.getKey(), entry.getValue());
                }
            }
        }
        setItems(itemsToShow.values());
        setEnabled(!itemsToShow.isEmpty());
        T item = itemsToShow.get(primary + secondary);
        if (!isEmptySelectionAllowed() && item == null) {
            item = itemsToShow.values().stream().filter(HasPrimarySecondaryMain::mainItem).findFirst().orElse(null);
        }
        setValue(item);
        getElement().setProperty("title", item != null ? itemLabelGenerator.apply(item) : "");
    }

    /**
     * Returns whether the internal items map contains any entries.
     *
     * @return {@code true} if at least one item is present
     */
    public boolean hasAvailableItems() {
        return !items.isEmpty();
    }

    /**
     * Returns the item marked as main, or the first available item if none is marked as main.
     *
     * @return the main item, the first item, or {@code null} if the map is empty
     */
    public T gatMainOrFirst() {
        T main = items.get("main");
        return main != null ? main : items.values().stream().findFirst().orElse(null);
    }
}
