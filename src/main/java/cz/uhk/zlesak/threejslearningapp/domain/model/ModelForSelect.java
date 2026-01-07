package cz.uhk.zlesak.threejslearningapp.domain.model;

import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondary;

public record ModelForSelect(String id, String mainTextureId, String modelName, boolean main) implements HasPrimarySecondary {
    /**
     * @return returns the primary value, which is the model ID.
     */
    @Override
    public String primary() {
        return id;
    }

    /**
     * @return returns the secondary value, which is an empty string.
     */
    @Override
    public String secondary() {
        return "";
    }
}
