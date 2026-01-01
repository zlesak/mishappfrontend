package cz.uhk.zlesak.threejslearningapp.domain.texture;

/**
 * Record class that represents a texture listing for a select item.
 * Used for hte selection of the applied texture in the ThreeJS renderer.
 *
 * @param textureId for the texture identifier, typically a unique identifier for the texture area.
 * @param textureName for the name of the texture area, which is displayed in the combo box.
 */
public record TextureListingForSelect(String textureId, String modelId, String textureName, boolean... main) {
}
