package cz.uhk.zlesak.threejslearningapp.components.selects;

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

@Scope("prototype")
public class TextureAreaSelect extends GenericSelect<TextureAreaForSelect> {

    public TextureAreaSelect() {
        super("textureAreaSelect.caption", area -> area.areaName() != null ? area.areaName() : "", TextureAreaForSelect.class, true);
    }

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

    @Override
    public void handleFileUploadIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
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
