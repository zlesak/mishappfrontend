package cz.uhk.zlesak.threejslearningapp.common;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TextureMapHelper is a utility class that provides methods for creating maps of texture data.
 */
public abstract class TextureMapHelper {

    /**
     * Creates a list of TextureAreaForSelectRecord from the provided map of QuickModelEntity objects.
     * Removes duplicate texture areas - if the same model is used for multiple sub-chapters, its areas appear only once.
     *
     * @param quickModelEntityMap the map of QuickModelEntity objects
     * @return a list of TextureAreaForSelectRecord objects (without duplicates)
     * @throws IllegalArgumentException if the CSV content format is invalid
     */
    public static List<TextureAreaForSelect> createTextureAreaForSelectRecordList(Map<String, QuickModelEntity> quickModelEntityMap) {
        List<TextureAreaForSelect> records = new ArrayList<>();

        Map<String, QuickModelEntity> uniqueModels = new java.util.LinkedHashMap<>();
        quickModelEntityMap.values().forEach(model -> {
            if (model != null) {
                uniqueModels.putIfAbsent(model.getModel().getId(), model);
            }
        });

        for (QuickModelEntity modelEntity : uniqueModels.values()) {
            List<QuickTextureEntity> allTextures = modelEntity.getOtherTextures();
            if (allTextures == null || allTextures.isEmpty()) continue;
            for (QuickTextureEntity textureEntity : allTextures) {
                if (textureEntity == null) continue;
                String textureId = textureEntity.getId();
                String csvContent = textureEntity.getCsvContent();
                if (csvContent == null || csvContent.isEmpty()) continue;
                csvParse(modelEntity.getModel().getId(), csvContent, records, textureId);
            }
        }
        return records;
    }

    /**
     * Parses CSV content and adds TextureAreaForSelect records to the provided list.
     * @param modelId the model ID
     * @param csvContent the CSV content to parse
     * @param records the list to add records to
     * @param textureId the texture ID
     * @throws IllegalArgumentException if the CSV content format is invalid
     */
    public static void csvParse(String modelId, String csvContent, List<TextureAreaForSelect> records, String textureId) {
        String[] rows = csvContent.split("\\r?\\n|\\r");
        for (String row : rows) {
            row = row.trim();
            if (row.isEmpty()) continue;
            String[] parts = row.split(";");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid CSV format for TextureAreaForComboBoxRecord: " + row);
            }
            records.add(new TextureAreaForSelect(textureId, parts[0].trim(), parts[1].trim(), modelId));
        }
    }
}
