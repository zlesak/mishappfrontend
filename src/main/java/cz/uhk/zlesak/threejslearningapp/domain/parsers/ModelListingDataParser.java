package cz.uhk.zlesak.threejslearningapp.domain.parsers;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class ModelListingDataParser {

    /**
     * Parses model data for selection lists, prioritizing the 'main' model if present.
     * Removes duplicate models - if the same model is used for multiple sub-chapters, it appears only once.
     *
     * @param models Map of model keys to QuickModelEntity objects
     * @return List of ModelForSelectRecord objects for selection (without duplicates)
     */
    public static List<ModelForSelect> modelForSelectDataParser(Map<String, QuickModelEntity> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }

        Map<String, ModelForSelect> uniqueModels = new LinkedHashMap<>();

        models.forEach((key, entity) -> {
            String modelId = entity.getModel().getId();
            String textureId = entity.getMainTexture() != null ? entity.getMainTexture().getTextureFileId() : null;
            if (!uniqueModels.containsKey(modelId)) {
                uniqueModels.put(modelId, new ModelForSelect(
                        modelId,
                        textureId,
                        entity.getModel().getName(),
                        "main".equals(key)
                ));
            }
        });

        return new LinkedList<>(uniqueModels.values());
    }
}
