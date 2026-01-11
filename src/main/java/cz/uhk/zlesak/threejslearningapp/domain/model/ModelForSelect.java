package cz.uhk.zlesak.threejslearningapp.domain.model;

import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondaryMain;

/**
 * ModelForSelect record - represents a simplified model for selection purposes.
 * Implements HasPrimarySecondaryMain to provide primary, secondary, and main item information.
 * @see HasPrimarySecondaryMain for details.
 *
 * @param id the unique identifier of the model
 * @param mainTextureId the identifier of the main texture associated with the model
 * @param modelName the name of the model
 * @param mainItem indicates if this model is the main item
 */
public record ModelForSelect(String id, String mainTextureId, String modelName, boolean mainItem) implements HasPrimarySecondaryMain {
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
