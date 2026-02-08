package cz.uhk.zlesak.threejslearningapp.components.inputs.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * TextureAreaSelect is a specialized select component for choosing texture areas.
 * It extends GenericSelect and handles texture area selection changes and file upload events.
 */
@Scope("prototype")
public class TextureAreaSelect extends GenericSelect<TextureAreaForSelect, UploadFileEvent> {

    /**
     * Constructor for TextureAreaSelect.
     */
    public TextureAreaSelect() {
        super("textureAreaSelect.caption", area -> area.areaName() != null ? area.areaName() : "", TextureAreaForSelect.class, true);
    }

    /**
     * Creates a change event when the selected texture area changes.
     *
     * @param event the value change event
     * @return a ThreeJsActionEvent representing the apply mask to texture action
     */
    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<TextureAreaForSelect> event) {
        return new ThreeJsActionEvent(UI.getCurrent(),
                event.getValue() != null ? event.getValue().modelId() : event.getOldValue().modelId(),
                event.getValue() != null ? event.getValue().textureId() : event.getOldValue().modelId(),
                ThreeJsActions.APPLY_MASK_TO_TEXTURE,
                event.isFromClient(),
                questionId,
                event.getValue() != null ? event.getValue().hexColor() : null);
    }

    /**
     * Handles the addition of new texture areas when a file upload event occurs.
     *
     * @param uploadFileEvent the upload file event containing texture area information
     */
    @Override
    public void handleItemAdditionIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        List<TextureAreaForSelect> records = new ArrayList<>();
        TextureMapHelper.csvParse(
                uploadFileEvent.getModelId(),
                uploadFileEvent.getBase64File(),
                records,
                uploadFileEvent.getEntityId()
        );

        for (TextureAreaForSelect record : records) {
            String key = uploadFileEvent.getEntityId() + record.hexColor();
            items.putMultiple(key, record);
        }
    }
}
