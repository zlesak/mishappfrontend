package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEventListener;
import cz.uhk.zlesak.threejslearningapp.events.texture.TextureListingChangeEvent;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.parsers.TextureListingDataParser;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TextureListingSelect is a custom select for selecting texture listings.
 * It extends GenericSelect to provide functionality for handling texture listing selection changes.
 */
@Scope("prototype")
public class TextureListingSelect extends GenericSelect<TextureListingForSelect, TextureListingChangeEvent> {
    /**
     * Constructor for TextureListingSelect.
     * It initializes the select with an empty label, a text generator for items, and sets up the event handling for texture listing changes.
     * Calls the parent class constructor with the appropriate parameters.
     */
    public TextureListingSelect() {
        super("", TextureListingForSelect::textureName,
                TextureListingChangeEvent.class,
                (select, event) -> new TextureListingChangeEvent((TextureListingSelect) select, event.isFromClient(), event.getOldValue(), event.getValue()));
        setEmptySelectionAllowed(false);
        setWidthFull();
    }

    /**
     * Adds a listener for texture listing change events.
     * Calls the addGenericChangeListener method from the parent class to register the listener.
     *
     * @param listener the listener to be added
     */
    public void addTextureListingChangeListener(ComponentEventListener<TextureListingChangeEvent> listener) {
        addGenericChangeListener(listener);
    }

    /**
     * This method is used to populate the select with texture listing records.
     * Calls the initialize method from the parent class to set the items.
     *
     * @param models a map of model IDs to QuickModelEntity objects used to generate the texture listing records
     */
    public void initializeTextureListingSelect(Map<String, QuickModelEntity> models) {
        initialize(TextureListingDataParser.textureListingForSelectDataParser(models, true, text("textureListingSelect.noOtherTextures")), false);
    }

    /**
     * Sets the selected texture in the select based on the provided texture ID.
     * If the texture ID is null or not found, no selection is made.
     *
     * @param textureId the ID of the texture to be selected
     */
    public void setSelectedTextureById(String textureId) {
        if (textureId == null) return;
        for (TextureListingForSelect item : getItems()) {
            if (item.id().equals(textureId)) {
                setValue(item);
                return;
            }
        }
    }

    /**
     * Filters and shows only the textures associated with the specified model ID.
     * Updates the items in the select to only include those that match the given model ID.
     * Automatically selects the first texture in the filtered list.
     *
     * @param modelId the ID of the model whose textures should be displayed
     */
    public void showTexturesForSelectedModel(String modelId) {
        List<TextureListingForSelect> itemsToShow = new ArrayList<>();
        for (TextureListingForSelect item : this.getItems()) {
            if (item.modelId().equals(modelId)) {
                itemsToShow.add(item);
            }
        }
        setItems(itemsToShow);
        setSelectedTextureById(itemsToShow.getFirst().id());
    }
}
