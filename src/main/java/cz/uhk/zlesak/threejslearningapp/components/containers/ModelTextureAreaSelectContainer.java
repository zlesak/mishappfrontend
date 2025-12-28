package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.components.selects.ModelListingSelect;
import cz.uhk.zlesak.threejslearningapp.components.selects.TextureAreaSelect;
import cz.uhk.zlesak.threejslearningapp.components.selects.TextureListingSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import lombok.Getter;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.Objects;

/**
 * A component that combines texture listing and texture area selection for 3D rendering.
 * It allows users to select textures and apply them to specific areas of a 3D model.
 * The component interacts with a ThreeJsComponent to update the displayed texture based on user selections.
 *
 */
@Scope("prototype")
@Getter
public class ModelTextureAreaSelectContainer extends HorizontalLayout {
    ModelListingSelect modelListingSelect = new ModelListingSelect();
    TextureListingSelect textureListingSelect = new TextureListingSelect();
    TextureAreaSelect textureAreaSelect = new TextureAreaSelect();

    public ModelTextureAreaSelectContainer() {
        modelListingSelect.addModelChangeListener(
                event -> {
                    var newValue = event.getNewValue();
                    if (newValue != null) {
                        String modelId = newValue.id();
                        if (modelId != null && !Objects.equals(modelId, "")) {
                            ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelId, null, ThreeJsActions.SHOW_MODEL));
                            textureListingSelect.showTexturesForSelectedModel(modelId);
                        }
                    }
                }
        );
        textureListingSelect.addTextureListingChangeListener(
                event -> {
                    var newValue = event.getNewValue();
                    if (newValue != null) {
                        String textureId = newValue.id();

                        if (textureId != null && !Objects.equals(textureId, "")) {
                            ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), newValue.modelId(), textureId, ThreeJsActions.SWITCH_OTHER_TEXTURE));

                            textureAreaSelect.showSelectedTextureAreas(textureId);
                        }
                    }
                }
        );
        textureAreaSelect.addTextureAreaChangeListener(event -> {
            if (event.getNewValue() != null && !Objects.equals(event.getNewValue().textureId(), "")) {
                ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), event.getNewValue().modelId(), event.getNewValue().textureId(), ThreeJsActions.APPLY_MASK_TO_TEXTURE, event.getNewValue().hexColor()));

            }
        });
        add(modelListingSelect, textureListingSelect, textureAreaSelect);
        setVisible(false);
        setWidthFull();
    }

    public void initializeData(Map<String, QuickModelEntity> models) {
        textureAreaSelect.initializeTextureAreaSelect(models);
        textureListingSelect.initializeTextureListingSelect(models);
        modelListingSelect.initializeModelSelect(models);
        setVisible(true);
    }
}
