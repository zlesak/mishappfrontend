package cz.uhk.zlesak.threejslearningapp.domain.parsers;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * TextureListingDataParser class - utility class for parsing texture data into records for select.
 * This class provides a method to convert a list of QuickFileEntity objects into a list of TextureListingForSelectRecord objects.
 * Used for populating select with texture listings in the application.
 */
public abstract class TextureListingDataParser {
    /**
     * Parses texture listing data from a map of QuickModelEntity objects.
     * Removes duplicate textures - if the same model is used for multiple sub-chapters, its textures appear only once.
     *
     * @param models      Map of model IDs to QuickModelEntity objects.
     * @param allTextures Flag indicating whether to include all textures or only other textures.
     * @return List of TextureListingForSelectRecord objects (without duplicates).
     */
    public static List<TextureListingForSelect> textureListingForSelectDataParser(Map<String, QuickModelEntity> models, boolean allTextures, String noTextureText) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }

        Map<String, QuickModelEntity> uniqueModels = new LinkedHashMap<>();
        models.values().forEach(model -> uniqueModels.putIfAbsent(model.getModel().getId(), model));

        if (allTextures) {
            return uniqueModels.values().stream()
                    .flatMap(model -> {
                        String modelId = model.getModel().getId();
                        List<QuickTextureEntity> textures = model.getAllTextures();
                        if (textures == null || textures.isEmpty()) {
                            return Stream.of(new TextureListingForSelect(modelId, modelId, noTextureText));
                        }
                        return textures.stream()
                                .map(texture -> new TextureListingForSelect(
                                        texture.getTextureFileId(),
                                        modelId,
                                        texture.getName()));
                    })
                    .toList();
        } else {
            return uniqueModels.values().stream()
                    .flatMap(model -> {
                        String modelId = model.getModel().getId();
                        List<QuickTextureEntity> textures = model.getOtherTextures();
                        if (textures == null || textures.isEmpty()) {
                            return Stream.of(new TextureListingForSelect(modelId, modelId, noTextureText));
                        }
                        return textures.stream()
                                .map(texture -> new TextureListingForSelect(
                                        texture.getTextureFileId(),
                                        modelId,
                                        texture.getName()));
                    })
                    .toList();
        }
    }
}
