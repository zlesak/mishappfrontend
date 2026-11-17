package cz.uhk.zlesak.threejslearningapp.domain.model;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * A 3D model together with the files it is made of.
 *
 * <p>The binaries live in GridFS; this document holds the structure that says which file is the
 * model, which are its textures and which CSV annotates which texture. Keeping that structure with
 * the model means loading a model for display is a single read.
 *
 * @see AbstractEntity for common entity properties.
 * @see ModelEntity for the upload form's view of a model.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "models")
@TypeAlias("Model")
public class QuickModelEntity extends AbstractEntity {

    /** The model file and, beneath it, its textures and their CSV annotations. */
    ModelFileEntity model;

    /** Whether the model uses per-area textures, which changes how the viewer renders it. */
    boolean isAdvanced;

    QuickTextureEntity mainTexture;

    List<QuickTextureEntity> otherTextures;

    /**
     * @return all textures, with the main one first.
     */
    public List<QuickTextureEntity> getAllTextures() {
        List<QuickTextureEntity> allTextures = new ArrayList<>();
        if (otherTextures != null && !otherTextures.isEmpty()) {
            allTextures.addAll(otherTextures);
        }
        if (mainTexture != null) {
            allTextures.addFirst(mainTexture);
        }
        return allTextures;
    }
}
