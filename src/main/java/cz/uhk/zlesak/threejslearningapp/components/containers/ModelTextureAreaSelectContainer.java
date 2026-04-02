package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.components.inputs.selects.ModelListingSelect;
import cz.uhk.zlesak.threejslearningapp.components.inputs.selects.TextureAreaSelect;
import cz.uhk.zlesak.threejslearningapp.components.inputs.selects.TextureListingSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A component that combines texture listing and texture area selection for 3D rendering.
 * It allows users to select textures and apply them to specific areas of a 3D model.
 * The component interacts with a ThreeJsComponent to update the displayed texture based on user selections.
 *
 */
@Scope("prototype")
@Getter
@Slf4j
public class ModelTextureAreaSelectContainer extends HorizontalLayout {
    protected final List<Registration> registrations = new ArrayList<>();


    ModelListingSelect modelListingSelect = new ModelListingSelect();
    TextureListingSelect textureListingSelect = new TextureListingSelect();
    TextureAreaSelect textureAreaSelect = new TextureAreaSelect();

    String questionId;

    public ModelTextureAreaSelectContainer() {
        addClassName("model-texture-select-row");
        modelListingSelect.addClassName("model-texture-model-select");
        textureListingSelect.addClassName("model-texture-texture-select");
        textureAreaSelect.addClassName("model-texture-area-select");
        add(modelListingSelect, textureListingSelect, textureAreaSelect);
        setWidthFull();
        setWrap(true);
    }

    /**
     * Sets the question ID scoped to this container and propagates it to all child selects.
     *
     * @param questionId the question ID to associate with this container
     */
    public void setQuestionId(String questionId) {
        this.questionId = questionId;
        modelListingSelect.setQuestionId(questionId);
        textureListingSelect.setQuestionId(questionId);
        textureAreaSelect.setQuestionId(questionId);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                UploadFileEvent.class, e -> {
                    if ((e.getQuestionId() != null && this.questionId != null) && !e.getQuestionId().equals(this.questionId))
                        return;
                    switch (e.getFileType()) {
                        case MODEL -> {
                            modelListingSelect.handleItemAdditionIngoingChangeEventAction(e);
                            textureListingSelect.showRelevantItemsBasedOnContext(modelListingSelect.getValue() != null ? modelListingSelect.getValue().id() : "", "main"); //TODO MAIN
                        }
                        case MAIN, OTHER -> {
                            textureListingSelect.handleItemAdditionIngoingChangeEventAction(e);
                            modelListingSelect.showRelevantItemsBasedOnContext("", textureListingSelect.getValue() != null ? textureListingSelect.getValue().modelId() : "");
                        }
                        case CSV -> textureAreaSelect.handleItemAdditionIngoingChangeEventAction(e);
                    }
                    textureAreaSelect.showRelevantItemsBasedOnContext(textureListingSelect.getValue() != null ? textureListingSelect.getValue().textureId() : "", "");
                }
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                RemoveFileEvent.class, e -> {
                    if ((e.getQuestionId() != null && this.questionId != null) && !e.getQuestionId().equals(this.questionId))
                        return;
                    switch (e.getFileType()) {
                        case MODEL -> {
                            String oldValue = modelListingSelect.getValue() != null ? modelListingSelect.getValue().id() : "";
                            modelListingSelect.handleItemRemoveIngoingChangeEventAction(e.getModelId(), e.isFromClient());
                            if (modelListingSelect.hasAvailableItems()) {
                                modelListingSelect.showRelevantItemsBasedOnContext(Objects.equals(e.getModelId(), oldValue) ? modelListingSelect.gatMainOrFirst().id() : oldValue, "");
                                ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelListingSelect.getValue().id(), modelListingSelect.getValue().mainTextureId(), ThreeJsActions.SHOW_MODEL, e.isFromClient(), questionId));
                            }
                        }
                        case MAIN, OTHER ->
                                textureListingSelect.handleItemRemoveIngoingChangeEventAction(e.getEntityId(), e.isFromClient());
                        case CSV ->
                                textureAreaSelect.handleItemRemoveIngoingChangeEventAction(e.getEntityId(), e.isFromClient());
                    }
                    textureListingSelect.showRelevantItemsBasedOnContext(modelListingSelect.getValue() != null ? modelListingSelect.getValue().id() : null, "main");
                    textureAreaSelect.showRelevantItemsBasedOnContext(textureListingSelect.getValue() != null ? textureListingSelect.getValue().textureId() : null, "");
                }

        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsActionEvent.class, e -> {
                    if ((e.getQuestionId() != null && this.questionId != null) && !e.getQuestionId().equals(this.questionId))
                        return;
                    if (Objects.requireNonNull(e.getAction()) == ThreeJsActions.REMOVE) {
                        modelListingSelect.handleItemRemoveIngoingChangeEventAction(e.getModelId(), e.isFromClient());
                        textureListingSelect.handleItemRemoveIngoingChangeEventAction(e.getModelId() + e.getTextureId(), e.isFromClient());
                        textureAreaSelect.handleItemRemoveIngoingChangeEventAction(e.getTextureId() + e.getMaskColor(), e.isFromClient());
                    } else if (e.isFromClient()) {
                        modelListingSelect.showRelevantItemsBasedOnContext("", e.getModelId());
                        textureListingSelect.showRelevantItemsBasedOnContext(e.getModelId(), e.getTextureId());
                        textureAreaSelect.showRelevantItemsBasedOnContext(e.getTextureId(), e.getMaskColor());
                    }
                }
        ));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
