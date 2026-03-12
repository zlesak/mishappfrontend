package cz.uhk.zlesak.threejslearningapp.domain.texture;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * QuickTextureEntity Class - Represents a lightweight texture entity with essential information.
 * Extends AbstractEntity to inherit common properties.
 * @see AbstractEntity
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickTextureEntity extends AbstractEntity {
    String textureFileId;
    String modelId;
    Boolean isPrimary;
    String csvContent;
}
