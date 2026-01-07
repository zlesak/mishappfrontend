package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.springframework.context.annotation.Scope;

/**
 * ModelListingSelect is a specialized select component for choosing 3D models.
 * It extends GenericSelect and handles model selection changes and file upload events.
 */
@Scope("prototype")
public class ModelListingSelect extends GenericSelect<ModelForSelect, UploadFileEvent> {

    /**
     * Constructor for ModelListingSelect.
     */
    public ModelListingSelect() {
        super("modelSelect.caption", model -> model.modelName() != null ? model.modelName() : "", ModelForSelect.class, false);
    }

    /**
     * Creates a change event when the selected model changes.
     *
     * @param e the value change event
     * @return a ThreeJsActionEvent representing the show model action
     */
    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<ModelForSelect> e) {
        return new ThreeJsActionEvent(UI.getCurrent(), e.getValue().id(), e.getValue().mainTextureId(), ThreeJsActions.SHOW_MODEL, e.isFromClient(), questionId);
    }

    /**
     * Handles the addition of a new model when a file upload event occurs.
     *
     * @param uploadFileEvent the upload file event containing model information
     */
    @Override
    public void handleItemAdditionIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        items.putExtended(uploadFileEvent.getModelId(), new ModelForSelect(uploadFileEvent.getModelId(), uploadFileEvent.getEntityId(), uploadFileEvent.getFileName(), uploadFileEvent.isMain()), uploadFileEvent.isFromClient());
    }

}
