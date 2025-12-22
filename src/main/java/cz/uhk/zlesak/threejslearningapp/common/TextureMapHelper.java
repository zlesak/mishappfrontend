package cz.uhk.zlesak.threejslearningapp.common;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.services.TextureService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
            if (modelEntity.getOtherTextures() == null) continue;
            for (QuickTextureEntity textureEntity : modelEntity.getAllTextures()) {
                if (textureEntity == null) continue;
                String textureId = textureEntity.getTextureFileId();
                String csvContent = textureEntity.getCsvContent();
                if (csvContent == null || csvContent.isEmpty()) continue;
                String[] rows = csvContent.split("\\r?\\n|\\r");
                for (String row : rows) {
                    row = row.trim();
                    if (row.isEmpty()) continue;
                    String[] parts = row.split(";");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid CSV format for TextureAreaForComboBoxRecord: " + row);
                    }
                    records.add(new TextureAreaForSelect(textureId, parts[0].trim(), parts[1].trim(), modelEntity.getModel().getId()));
                }
            }
        }
        return records;
    }

    /**
     * Creates a map of texture file IDs to their corresponding texture stream endpoint URLs.
     * Uses the provided textureService to generate the URLs.
     * TODO Should be removed after BE provides endpoints in the QuickTextureEntity
     *
     * @param quickTextureEntityList the list of QuickTextureEntity objects
     * @param textureService         the textureService used to get the texture stream endpoint URLs
     * @return a map where the key is the texture file ID and the value is the texture stream endpoint URL
     * @throws IOException if an I/O error occurs while retrieving the texture stream endpoint URL
     */
    public static Map<String, String> otherTexturesMap(List<QuickTextureEntity> quickTextureEntityList, TextureService textureService) throws IOException {
        Map<String, String> otherTextures = new HashMap<>();
        for (QuickTextureEntity texture : quickTextureEntityList) {
            String textureUrl = textureService.getTextureFileBeEndpointUrl(texture.getTextureFileId());
            otherTextures.put(texture.getTextureFileId(), textureUrl);
        }
        return otherTextures;
    }
}
