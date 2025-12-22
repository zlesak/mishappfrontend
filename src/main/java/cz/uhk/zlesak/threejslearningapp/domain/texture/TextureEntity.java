package cz.uhk.zlesak.threejslearningapp.domain.texture;

import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * TextureEntity Class - Represents a texture entity with associated files.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TextureEntity extends QuickTextureEntity {
    InputStreamMultipartFile textureFile;
    InputStreamMultipartFile csvFile;
}