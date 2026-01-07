package cz.uhk.zlesak.threejslearningapp.components.selects;

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
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * GenericSelect is an abstract class extending Vaadin's Select component.
 * It is designed to handle items that implement the HasPrimarySecondary interface.
 * The class supports dynamic item display based on primary and secondary context,
 * and fires custom events on value changes.
 *
 * @param <T> the type of items in the select, extending HasPrimarySecondary
 * @param <E> the type of component event for item addition
 */
@Scope("prototype")
public abstract class GenericSelect<T extends HasPrimarySecondaryMain, E extends ComponentEvent<UI>> extends Select<T> implements I18nAware {
    @Setter
    protected String questionId;
    protected ObservableMap<String, T> items;

    /**
     * Constructor for GenericSelect.
     *
     * @param label               the label for the select component
     * @param itemLabelGenerator  the generator for item labels
     * @param itemType            the class type of the items
     * @param allowEmptySelection flag to allow empty selection
     */
    public GenericSelect(String label, ItemLabelGenerator<T> itemLabelGenerator, Class<T> itemType, boolean allowEmptySelection) {
        super();

        setEmptySelectionCaption(text(label));
        setWidthFull();
        setRenderer(new TextRenderer<>(itemLabelGenerator));
        if (itemType == TextureAreaForSelect.class) {
            setRenderer(new ComponentRenderer<>(item -> {
                Span span = new Span(itemLabelGenerator.apply(item));
                String color = ((TextureAreaForSelect) item).hexColor();
                if (color != null) {
                    span.getStyle().set("color", color);
                }
                return span;
            }));
        }
        items = new ObservableMap<>((value, fromClient) -> {
            if (fromClient)
                showRelevantItemsBasedOnContext(value != null ? value.primary() : null, value != null ? value.secondary() : null);
        }
        );
        addValueChangeListener(e -> {
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
        var itemsToShow = items.entrySet().stream()
                .filter(entry -> primary != null && entry.getKey().contains(primary))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        setItems(itemsToShow.values());
        setEnabled(!itemsToShow.isEmpty());
        T item = itemsToShow.get(primary + secondary);
        if(!isEmptySelectionAllowed() && item == null){
            item = itemsToShow.values().stream().filter(HasPrimarySecondaryMain::mainItem).findFirst().orElse(null);
        }
        setValue(item);
    }
}
