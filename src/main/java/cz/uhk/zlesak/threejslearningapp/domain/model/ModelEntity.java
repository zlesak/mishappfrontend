package cz.uhk.zlesak.threejslearningapp.domain.model;

import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;

import java.util.List;

/**
 * A model as the upload form sees it: the stored model plus the files being uploaded.
 * The file fields only exist while a model is being created or replaced and are never stored.
 *
 * @see QuickModelEntity for the stored form.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelEntity extends QuickModelEntity {

    @Transient
    InputStreamMultipartFile inputStreamMultipartFile;
    @Transient
    TextureEntity fullMainTexture;
    @Transient
    List<TextureEntity> fullOtherTextures;
    @Transient
    List<InputStreamMultipartFile> csvFiles;
    @Transient
    InputStreamMultipartFile backgroundImageFile;

    /**
     * Set on a version put together in the conflict dialog when the user kept the stored files:
     * the save then changes only the name and leaves the stored files, thumbnail and background as
     * they are, instead of uploading anything.
     */
    @Transient
    boolean keepStoredFiles;

    /**
     * Wraps a stored model so a detail screen can work with it.
     *
     * @param stored model loaded from the database, may be {@code null}.
     * @return the same model as a {@link ModelEntity}, or {@code null}.
     */
    public static ModelEntity of(QuickModelEntity stored) {
        if (stored == null) {
            return null;
        }
        return ModelEntity.builder()
                .id(stored.getId())
                .name(stored.getName())
                .creatorId(stored.getCreatorId())
                .description(stored.getDescription())
                .created(stored.getCreated())
                .updated(stored.getUpdated())
                .model(stored.getModel())
                .isAdvanced(stored.isAdvanced())
                .mainTexture(stored.getMainTexture())
                .otherTextures(stored.getOtherTextures())
                .build();
    }
}
