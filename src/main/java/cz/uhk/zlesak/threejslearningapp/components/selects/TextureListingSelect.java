package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.springframework.context.annotation.Scope;

@Scope("prototype")
public class TextureListingSelect extends GenericSelect<TextureListingForSelect> {

    public TextureListingSelect() {
        super("textureListingSelect.caption", texture -> texture.textureName() != null ? texture.textureName() : "", TextureListingForSelect.class, false);
    }

    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<TextureListingForSelect> e) {
        return new ThreeJsActionEvent(UI.getCurrent(), e.getValue().modelId(), e.getValue().textureId(), ThreeJsActions.SWITCH_OTHER_TEXTURE, e.isFromClient(), questionId);
    }

    @Override
    public void handleFileUploadIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        items.putExtended(uploadFileEvent.getModelId() + uploadFileEvent.getEntityId(), new TextureListingForSelect(uploadFileEvent.getEntityId(), uploadFileEvent.getModelId(), uploadFileEvent.getFileName(), uploadFileEvent.getFileType() == FileType.MAIN), uploadFileEvent.isFromClient());
    }
}
