package cz.uhk.zlesak.threejslearningapp.domain.texture;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * TextureUpload Class - Represents a texture upload entity to be sent to the BE for uploading a texture.
 * Extends TextureEntity to inherit common texture properties.
 * @see TextureEntity
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TextureUpload extends TextureEntity{
    TextureEntity texture;
}
