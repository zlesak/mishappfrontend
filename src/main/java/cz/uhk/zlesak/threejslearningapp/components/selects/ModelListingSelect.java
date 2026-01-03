package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubChapterChangeEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.springframework.context.annotation.Scope;

import java.util.Objects;

/**
 * ModelListingSelect is a custom select component for choosing 3D models.
 * It extends GenericSelect with ModelForSelectRecord as the item type and ModelListingChangeEvent as the event type.
 * It provides functionality to initialize the select with a list of models and handle model selection changes.
 */
@Scope("prototype")
public class ModelListingSelect extends GenericSelect<ModelForSelect> {
    /**
     * Constructor to initialize the ModelListingSelect component.
     * Sets up the select with appropriate item label generator and change event handling.
     */
    public ModelListingSelect() {
        super("", model -> model.modelName() != null ? model.modelName() : "", ModelForSelect.class, true, ModelForSelect::id);
        setEmptySelectionCaption(text("modelSelect.caption"));
        setEmptySelectionAllowed(false);
        setWidthFull();
        setEnabled(false);
    }

    @Override
    public void showRelevantItemsBasedOnContext(String modelId, ModelForSelect additionalContext, boolean fromClient, String... specificEntityId) {
        modelId = modelId != null ? modelId : additionalContext != null ? additionalContext.id() : null;
        setEnabled(!items.isEmpty());

        if (!items.isEmpty() && items.containsKey(modelId) && getValue() != null && Objects.equals(getValue().id(), modelId))
            return;

        model = null;
        setItems(items.values());

        ModelForSelect main = items.values().stream().filter(ModelForSelect::main).findFirst().orElse(null);
        if ((!fromClient || Objects.equals(modelId, "main")) && main != null) {
            model = modelId;
            setValue(main);
            return;
        }
        for (ModelForSelect item : items.values()) {
            if (item.id().equals(modelId)) {
                model = modelId;
                setValue(item);
                return;
            }
        }
        setValue(null);
    }

    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<ModelForSelect> event) {
        ModelForSelect e = event.getValue() != null ? event.getValue() : null;
        return new ThreeJsActionEvent(UI.getCurrent(), e != null ? e.id() : null, "main", ThreeJsActions.SHOW_MODEL, event.isFromClient(), questionId, "main", event.getValue() == null ? "main" : null);
    }

    @Override
    protected void handleFileUploadIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        if (uploadFileEvent.getFileType() == FileType.MODEL && (questionId == null || Objects.equals(uploadFileEvent.getQuestionId(), questionId))) {
            items.putExtended(uploadFileEvent.getModelId(), new ModelForSelect(uploadFileEvent.getModelId(), uploadFileEvent.getFileName(), uploadFileEvent.isMain()), uploadFileEvent.isFromClient());
        }
    }

    @Override
    protected void handleFileRemoveIngoingChangeEventAction(RemoveFileEvent removeFileEvent) {
        if (removeFileEvent.getFileType() == FileType.MODEL && (questionId == null || Objects.equals(removeFileEvent.getQuestionId(), questionId))) {
            items.remove(removeFileEvent.getEntityId());
        }
    }

    @Override
    protected void handleIngoingActionChangeEventAction(ThreeJsActionEvent threeJsActionEvent) {
        if(questionId == null || Objects.equals(threeJsActionEvent.getQuestionId(), questionId)) {
            if (threeJsActionEvent.getAction() == ThreeJsActions.REMOVE) {
                items.remove(threeJsActionEvent.getModelId());
                return;
            }
            if (threeJsActionEvent.isFromClient()) {
                String modelId = threeJsActionEvent.getModelId() != null ? threeJsActionEvent.getModelId() : "main";
                showRelevantItemsBasedOnContext(modelId, null, true);
            }
        }
    }

    @Override
    protected void handleSubChapterChangeEventAction(SubChapterChangeEvent subChapterChangeEvent) {

    }

}
