package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubChapterChangeEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TextureListingSelect is a custom select for selecting texture listings.
 * It extends GenericSelect to provide functionality for handling texture listing selection changes.
 */
@Scope("prototype")
public class TextureListingSelect extends GenericSelect<TextureListingForSelect> {

    /**
     * Constructor for TextureListingSelect.
     * It initializes the select with an empty label, a text generator for items, and sets up the event handling for texture listing changes.
     * Calls the parent class constructor with the appropriate parameters.
     */
    public TextureListingSelect() {
        super("", texture -> texture.textureName() != null ? texture.textureName() : "", TextureListingForSelect.class, true, TextureListingForSelect::textureId);
        setEmptySelectionCaption(text("textureListingSelect.caption"));
        setEmptySelectionAllowed(false);
        setWidthFull();
        setEnabled(false);
    }

    @Override
    public void showRelevantItemsBasedOnContext(String modelId, TextureListingForSelect additionalContext, boolean fromClient, String... specificEntityId) {
        String textureId = (specificEntityId != null && specificEntityId.length > 0) ? specificEntityId[0] : additionalContext != null ? additionalContext.modelId() : null;

        if (Objects.equals(modelId, model) && Objects.equals(textureId, texture))
            return;

        texture = null;
        List<TextureListingForSelect> itemsToShow = new ArrayList<>();
        modelId = modelId != null ? modelId : additionalContext != null ? additionalContext.modelId() : null;

        if (fromClient) {
            if (modelId != null) {
                String finalModelId = modelId;
                itemsToShow = items.values().stream().filter(e -> e.modelId().equals(finalModelId)).toList();
                setItems(itemsToShow);
                if (specificEntityId != null && specificEntityId.length > 0 && specificEntityId[0] != null && !specificEntityId[0].equals("main")) {
                    TextureListingForSelect specific = itemsToShow.stream().filter(e -> Objects.equals(e.textureId(), specificEntityId[0])).findFirst().orElse(null);
                    if (specific != null) {
                        texture = specific.textureId();
                        setValue(specific);
                    }
                } else if (additionalContext != null && itemsToShow.contains(additionalContext)) {
                    texture = additionalContext.textureId();
                    setValue(additionalContext);
                } else {
                    TextureListingForSelect mainTexture = itemsToShow.stream().filter(e -> e.main()[0]).findFirst().orElse(null);
                    if (mainTexture != null) {
                        texture = mainTexture.textureId();
                        setValue(mainTexture);
                    }
                }
            }
        }
        setEnabled(!itemsToShow.isEmpty());
    }

    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<TextureListingForSelect> event) {
        TextureListingForSelect e = event.getValue() != null ? event.getValue() : null;
        return new ThreeJsActionEvent(UI.getCurrent(), e != null ? e.modelId() : null, e != null ? e.textureId() : null, ThreeJsActions.SWITCH_OTHER_TEXTURE, event.isFromClient(), questionId);
    }

    @Override
    protected void handleFileUploadIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        switch (uploadFileEvent.getFileType()) {
            case MAIN, OTHER ->
                    items.putExtended(uploadFileEvent.getEntityId(), new TextureListingForSelect(uploadFileEvent.getEntityId(), uploadFileEvent.getModelId(), uploadFileEvent.getFileName(), uploadFileEvent.getFileType() == FileType.MAIN), uploadFileEvent.isFromClient());
        }
    }

    @Override
    protected void handleFileRemoveIngoingChangeEventAction(RemoveFileEvent removeFileEvent) {
        switch (removeFileEvent.getFileType()) {
            case MAIN, OTHER -> items.remove(removeFileEvent.getEntityId());
        }
    }

    @Override
    protected void handleIngoingActionChangeEventAction(ThreeJsActionEvent threeJsActionEvent) {
        if(questionId == null || Objects.equals(threeJsActionEvent.getQuestionId(), questionId)) {
            if (threeJsActionEvent.getAction() == ThreeJsActions.REMOVE) {
                items.remove(threeJsActionEvent.getModelId());
                return;
            }
            if (threeJsActionEvent.isFromClient() || threeJsActionEvent.isForceClient()) {
                showRelevantItemsBasedOnContext(threeJsActionEvent.getModelId(), null, true, threeJsActionEvent.getTextureId());
            }
        }
    }

    @Override
    protected void handleSubChapterChangeEventAction(SubChapterChangeEvent subChapterChangeEvent) {

    }
}
