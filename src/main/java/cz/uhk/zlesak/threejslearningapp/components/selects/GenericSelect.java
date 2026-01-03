package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.common.ObservableMap;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubChapterChangeEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Setter;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * GenericSelect abstract class extending Vaadin Select component.
 * It provides a generic implementation for select components with custom event handling.
 *
 * @param <T>
 */
@Scope("prototype")
public abstract class GenericSelect<T> extends Select<T> implements I18nAware {
    protected final List<Registration> registrations = new ArrayList<>();
    protected final Function<T, String> idGenerator;
    @Setter
    protected String questionId;

    protected ObservableMap<String, T> items;

    protected static String model = null;
    protected static String texture = null;
    protected static String area = null;

    /**
     * Constructor for GenericSelect.
     *
     * @param label                  Select label
     * @param itemLabelGenerator     label generator for items
     * @param itemType               class type of the items
     * @param setFirstItemAsSelected whether to set the first item as selected
     */
    public GenericSelect(String label, ItemLabelGenerator<T> itemLabelGenerator, Class<T> itemType, boolean setFirstItemAsSelected, Function<T, String> idGenerator) {
        super();
        this.idGenerator = idGenerator;

        setLabel(label);
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
        items = new ObservableMap<>((value, fromClient) -> showRelevantItemsBasedOnContext(null, value, fromClient)
        );
        addValueChangeListener(e ->
                ComponentUtil.fireEvent(UI.getCurrent(), createChangeEvent(e))
        );
    }

    abstract protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<T> event);

    abstract protected void handleFileUploadIngoingChangeEventAction(UploadFileEvent fileType);

    abstract protected void handleFileRemoveIngoingChangeEventAction(RemoveFileEvent fileType);

    abstract protected void handleIngoingActionChangeEventAction(ThreeJsActionEvent threeJsActionEvent);

    abstract protected void handleSubChapterChangeEventAction(SubChapterChangeEvent subChapterChangeEvent);

    abstract protected void showRelevantItemsBasedOnContext(String entityId, T additionalContext, boolean fromClient, String... specificEntityId);

    protected void initialize(List<T> itemsToInitialize) {
        items.clear();
        clear();
        for (T item : itemsToInitialize) {
            String key = idGenerator.apply(item);
            items.putExtended(key, item, false);
        }
        setItems(items.values());
        setEmptySelectionAllowed(true);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                UploadFileEvent.class,
                this::handleFileUploadIngoingChangeEventAction
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                RemoveFileEvent.class,
                this::handleFileRemoveIngoingChangeEventAction

        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsActionEvent.class,
                this::handleIngoingActionChangeEventAction

        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                SubChapterChangeEvent.class,
                this::handleSubChapterChangeEventAction
        ));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
