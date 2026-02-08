package cz.uhk.zlesak.threejslearningapp.domain.texture;

import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondaryMain;

/**
 * Record representing a texture area for a select component.
 * This record is used to store the name of the area and its corresponding hex color.
 * Implements HasPrimarySecondaryMain to provide primary, secondary, and main item information.
 * @see HasPrimarySecondaryMain for more details.
 *
 * @param textureId The identifier of the texture.
 * @param hexColor The hex color associated with the texture area.
 * @param areaName The name of the texture area.
 * @param modelId The identifier of the model associated with the texture area.
 */
public record TextureAreaForSelect(String textureId, String hexColor, String areaName, String modelId) implements HasPrimarySecondaryMain {
    /**
     * @return returns the primary value, which is the texture ID.
     */
    @Override
    public String primary() {
        return textureId;
    }

    /**
     * @return returns the secondary value, which is the hex color.
     */
    @Override
    public String secondary() {
        return hexColor;
    }

    /**
     * @return returns if this is a main item, which is always false in this context.
     */
    @Override
    public boolean mainItem() {
        return false;
    }
}
