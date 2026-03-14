package cz.uhk.zlesak.threejslearningapp.domain.model;

/**
 * Enum representing the type of a file in the model hierarchy.
 * Used to distinguish between the main model file, main texture, other textures, and CSV files for advanced texture mapping.
 */
public enum FileSenseType {
    MODEL,
    MAIN_TEXTURE,
    OTHER_TEXTURE,
    CSV_FILE,
    BACKGROUND_IMAGE
}
