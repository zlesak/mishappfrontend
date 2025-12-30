package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.components.selects.ModelListingSelect;
import cz.uhk.zlesak.threejslearningapp.components.selects.TextureAreaSelect;
import cz.uhk.zlesak.threejslearningapp.components.selects.TextureListingSelect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

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

    ModelListingSelect modelListingSelect = new ModelListingSelect();
    TextureListingSelect textureListingSelect = new TextureListingSelect();
    TextureAreaSelect textureAreaSelect = new TextureAreaSelect();

    public ModelTextureAreaSelectContainer() {
        add(modelListingSelect, textureListingSelect, textureAreaSelect);
        setWidthFull();
    }
}
