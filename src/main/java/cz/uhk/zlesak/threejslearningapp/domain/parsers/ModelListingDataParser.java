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

        if (models.containsKey("main")) {
            QuickModelEntity mainEntity = models.get("main");
            uniqueModels.put(mainEntity.getModel().getId(), new ModelForSelect(
                    mainEntity.getModel().getId(),
                    "main",
                    mainEntity.getModel().getName()
            ));
        }

        models.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("main"))
                .forEach(entry -> {
                    String modelId = entry.getValue().getModel().getId();
                    if (!uniqueModels.containsKey(modelId)) {
                        uniqueModels.put(modelId, new ModelForSelect(
                                modelId,
                                entry.getKey(),
                                entry.getValue().getModel().getName()
                        ));
                    }
                });

        return new LinkedList<>(uniqueModels.values());
    }
}
