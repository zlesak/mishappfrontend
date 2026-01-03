package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
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
 * TextureAreaSelect is a custom select implementation for selecting texture areas to be shown in the renderer.
 * It extends GenericSelect to provide functionality for handling texture area selection changes.
 */
@Scope("prototype")
public class TextureAreaSelect extends GenericSelect<TextureAreaForSelect> {
    String wantedAreaHexColor = null;

    /**
     * Constructor for TextureAreaSelect.
     * It initializes the select with an empty label, a text generator for items, and sets up the event handling for texture area changes.
     */
    public TextureAreaSelect() {
        super("", area -> area.areaName() != null ? area.areaName() : "", TextureAreaForSelect.class, false, TextureAreaForSelect::textureId);
        setEmptySelectionAllowed(true);
        setEmptySelectionCaption(text("textureAreaSelect.caption"));
        setWidthFull();
        setEnabled(false);
    }

    /**
     * Filters the displayed texture areas based on the provided texture ID.
     * If the texture ID is null, all items are shown.
     *
     * @param textureId the ID of the texture to filter by
     */
    @Override
    public void showRelevantItemsBasedOnContext(String textureId, TextureAreaForSelect additionalContext, boolean fromClient, String... specificEntityId) {
        List<TextureAreaForSelect> itemsToShow = new ArrayList<>();

        if (fromClient) {
            textureId = textureId != null ? textureId : additionalContext != null ? additionalContext.textureId() : null;

            if ((textureId != null && !Objects.equals(textureId, texture))) {
                wantedAreaHexColor = (specificEntityId != null && specificEntityId.length > 0 && specificEntityId[0] != null) ? specificEntityId[0] : null;
                return;
            }

            String finalTextureId = textureId != null ? textureId : texture;

            area = null;

            itemsToShow = items.values().stream().filter(e -> e.textureId().equals(finalTextureId)).toList();
            setItems(itemsToShow);

            String areaId = wantedAreaHexColor != null ? wantedAreaHexColor : (specificEntityId.length > 0 && specificEntityId[0] != null) ? specificEntityId[0] : null;
            if (areaId != null) {
                TextureAreaForSelect specific = itemsToShow.stream().filter(e -> Objects.equals(e.hexColor(), areaId)).findFirst().orElse(null);
                if (specific != null) {
                    area = specific.hexColor();
                    setValue(specific);
                    wantedAreaHexColor = null;
                }
            }
        }
        setEnabled(!itemsToShow.isEmpty());
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
    protected void handleFileUploadIngoingChangeEventAction(UploadFileEvent uploadFileEvent) {
        if (Objects.requireNonNull(uploadFileEvent.getFileType()) == FileType.CSV) {
            List<TextureAreaForSelect> records = new ArrayList<>();
            TextureMapHelper.csvParse(
                    uploadFileEvent.getModelId(),
                    uploadFileEvent.getBase64File(),
                    records,
                    uploadFileEvent.getEntityId()
            );
            String commonPart = uploadFileEvent.getFileName() + uploadFileEvent.getModelId();
            for (TextureAreaForSelect record : records) {
                String key = commonPart + "_" + record.hexColor();
                items.putMultiple(key, record);
            }
            items.notifyChange(items.keySet().stream().filter(e -> e.contains(commonPart)).findFirst().map(items::get).orElse(null), uploadFileEvent.isFromClient());
        }
    }

    @Override
    protected void handleFileRemoveIngoingChangeEventAction(RemoveFileEvent removeFileEvent) {
        if (Objects.requireNonNull(removeFileEvent.getFileType()) == FileType.CSV) {
            String commonPart = removeFileEvent.getEntityId() + removeFileEvent.getModelId();
            items.keySet().removeIf(k -> k.startsWith(commonPart + "_"));
            items.notifyChange(null, removeFileEvent.isFromClient());
        }
    }

    @Override
    protected void handleIngoingActionChangeEventAction(ThreeJsActionEvent threeJsActionEvent) {
        if(questionId == null || Objects.equals(threeJsActionEvent.getQuestionId(), questionId)) {
            if (threeJsActionEvent.getAction() == ThreeJsActions.REMOVE)
            {
                String commonPart = threeJsActionEvent.getModelId();
                items.keySet().removeIf(k -> k.contains(commonPart));
                items.notifyChange(null, threeJsActionEvent.isFromClient());
                return;
            }
            if (threeJsActionEvent.isFromClient() || threeJsActionEvent.getAction() == ThreeJsActions.SWITCH_OTHER_TEXTURE || threeJsActionEvent.getAction() == ThreeJsActions.SWITCH_MAIN_TEXTURE) {
                showRelevantItemsBasedOnContext(threeJsActionEvent.getTextureId(), null, true, threeJsActionEvent.getMaskColor());
            }
        }
    }

    @Override
    protected void handleSubChapterChangeEventAction(SubChapterChangeEvent subChapterChangeEvent) {

    }
}
