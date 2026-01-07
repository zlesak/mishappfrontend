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
import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondary;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Setter;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.stream.Collectors;

@Scope("prototype")
public abstract class GenericSelect<T extends HasPrimarySecondary> extends Select<T> implements I18nAware {
    @Setter
    protected String questionId;
    protected ObservableMap<String, T> items;

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
            if (fromClient) showRelevantItemsBasedOnContext(value != null ? value.primary() : null, value != null ? value.secondary() : null);
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

    abstract protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<T> event);

    abstract public void handleFileUploadIngoingChangeEventAction(UploadFileEvent fileType);

    public void handleFileRemoveIngoingChangeEventAction(String id, boolean fromClient) {
        items.keySet().removeIf(k -> k.contains(id));
        items.notifyChange(null, fromClient);
    }

    public void showRelevantItemsBasedOnContext(String primary, String secondary) {
        var itemsToShow = items.entrySet().stream()
            .filter(entry -> primary != null && entry.getKey().contains(primary)) //&& !primary.isEmpty()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        setItems(itemsToShow.values());
        setEnabled(!itemsToShow.isEmpty());
        setValue(itemsToShow.get(primary + secondary));
    }
}
