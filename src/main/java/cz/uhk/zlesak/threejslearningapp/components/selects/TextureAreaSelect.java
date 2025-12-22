package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEventListener;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.events.texture.TextureAreaChangeEvent;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TextureAreaSelect is a custom select implementation for selecting texture areas to be shown in the renderer.
 * It extends GenericSelect to provide functionality for handling texture area selection changes.
 */
@Scope("prototype")
public class TextureAreaSelect extends GenericSelect<TextureAreaForSelect, TextureAreaChangeEvent> {
    /**
     * Constructor for TextureAreaSelect.
     * It initializes the select with an empty label, a text generator for items, and sets up the event handling for texture area changes.
     */
    public TextureAreaSelect() {
        super("", TextureAreaForSelect::areaName,
                TextureAreaChangeEvent.class,
                (select, event) -> new TextureAreaChangeEvent((TextureAreaSelect) select, event.isFromClient(), event.getOldValue(), event.getValue()));
        setEmptySelectionAllowed(true);
        setEmptySelectionCaption(text("textureAreaSelect.caption"));
        setWidthFull();
    }

    /**
     * Adds a listener for texture area change events.
     * Calls the addGenericChangeListener method from the parent class to register the listener.
     *
     * @param listener the listener to be added
     */
    public void addTextureAreaChangeListener(ComponentEventListener<TextureAreaChangeEvent> listener) {
        addGenericChangeListener(listener);
    }

    /**
     * This method is used to populate the select with texture area records.
     * Calls the initialize method from the parent class to set the items.
     *
     * @param models a map of model IDs to QuickModelEntity objects used to generate the texture area records
     */
    public void initializeTextureAreaSelect(Map<String, QuickModelEntity> models) {
        initialize(TextureMapHelper.createTextureAreaForSelectRecordList(models), false);
    }

    /**
     * Filters the displayed texture areas based on the provided texture ID.
     * If the texture ID is null, all items are shown.
     *
     * @param textureId the ID of the texture to filter by
     */
    public void showSelectedTextureAreas(String textureId) {
        List<TextureAreaForSelect> itemsToShow = new ArrayList<>();
        for (TextureAreaForSelect item : this.getItems()) {
            if (item.textureId().equals(textureId)) {
                itemsToShow.add(item);
            }
        }
        setItems(itemsToShow);
    }

    /**
     * Sets the selected area based on the provided hex color and texture ID.
     * If no matching area is found, the selection remains unchanged.
     *
     * @param hexColor  the hex color of the area to select
     * @param textureId the ID of the texture associated with the area
     */
    public void setSelectedAreaByHexColor(String hexColor, String textureId) {
        if (hexColor == null) return;
        for (TextureAreaForSelect item : getItems()) {
            if (item.hexColor().equals(hexColor) && item.textureId().equals(textureId)) {
                setValue(item);
                return;
            }
        }
    }
}
