package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.springframework.context.annotation.Scope;

/**
 * TextureListingSelect is a specialized select component for choosing texture listings.
 * It extends GenericSelect and handles texture selection changes and file upload events.
 */
@Scope("prototype")
public class TextureListingSelect extends GenericSelect<TextureListingForSelect, UploadFileEvent> {

    /**
     * Constructor for TextureListingSelect.
     */
    public TextureListingSelect() {
        super("textureListingSelect.caption", texture -> texture.textureName() != null ? texture.textureName() : "", TextureListingForSelect.class, false);
    }

    /**
     * Creates a change event when the selected texture listing changes.
     *
     * @param e the value change event
     * @return a ThreeJsActionEvent representing the texture switch action
     */
    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<TextureListingForSelect> e) {
        return new ThreeJsActionEvent(UI.getCurrent(), e.getValue().modelId(), e.getValue().textureId(), ThreeJsActions.SWITCH_OTHER_TEXTURE, e.isFromClient(), questionId);
    }

    /**
     * Handles the addition of a new texture listing when a file upload event occurs.
     *
     * @param uploadFileEvent the upload file event containing texture information
     */
    @Override
    public void handleItemAdditionIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        items.putExtended(uploadFileEvent.getModelId() + uploadFileEvent.getEntityId(), new TextureListingForSelect(uploadFileEvent.getEntityId(), uploadFileEvent.getModelId(), uploadFileEvent.getFileName(), uploadFileEvent.getFileType() == FileType.MAIN), uploadFileEvent.isFromClient());
    }
}
