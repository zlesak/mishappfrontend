package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.springframework.context.annotation.Scope;

@Scope("prototype")
public class ModelListingSelect extends GenericSelect<ModelForSelect> {

    public ModelListingSelect() {
        super("modelSelect.caption", model -> model.modelName() != null ? model.modelName() : "", ModelForSelect.class, false);
    }

    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<ModelForSelect> e) {
        return new ThreeJsActionEvent(UI.getCurrent(), e.getValue().id(), e.getValue().mainTextureId(), ThreeJsActions.SHOW_MODEL, e.isFromClient(), questionId);
    }

    @Override
    public void handleFileUploadIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        items.putExtended(uploadFileEvent.getModelId(), new ModelForSelect(uploadFileEvent.getModelId(), uploadFileEvent.getEntityId(),uploadFileEvent.getFileName(), uploadFileEvent.isMain()), uploadFileEvent.isFromClient());
    }

}
