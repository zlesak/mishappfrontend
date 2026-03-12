package cz.uhk.zlesak.threejslearningapp.domain.model;

import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Model entity data class - holds upload data and quick model metadata.
 * Upload-only fields (inputStreamMultipartFile, fullMainTexture, fullOtherTextures, csvFiles)
 * are populated when creating or updating a model. Quick metadata fields are inherited from QuickModelEntity.
 * @see QuickModelEntity
 */
@Data
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ModelEntity extends QuickModelEntity {
    InputStreamMultipartFile inputStreamMultipartFile;
    TextureEntity fullMainTexture;
    List<TextureEntity> fullOtherTextures;
    List<InputStreamMultipartFile> csvFiles;
}