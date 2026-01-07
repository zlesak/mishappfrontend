package cz.uhk.zlesak.threejslearningapp.domain.texture;

import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondary;

/**
 * Record class that represents a texture listing for a select item.
 * Used for hte selection of the applied texture in the ThreeJS renderer.
 *
 * @param textureId for the texture identifier, typically a unique identifier for the texture area.
 * @param textureName for the name of the texture area, which is displayed in the combo box.
 */
public record TextureListingForSelect(String textureId, String modelId, String textureName, boolean... main) implements HasPrimarySecondary {
    /**
     * @return returns the primary value, which is the model ID.
     */
    @Override
    public String primary() {
        return modelId;
    }

    /**
     * @return returns the secondary value, which is the texture ID.
     */
    @Override
    public String secondary() {
        return textureId;
    }
}
