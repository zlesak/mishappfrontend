package cz.uhk.zlesak.threejslearningapp.domain.texture;

import cz.uhk.zlesak.threejslearningapp.domain.file.QuickFileEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * QuickTextureEntity Class - Represents a lightweight texture entity with essential information.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickTextureEntity extends QuickFileEntity {
    String textureFileId;
    String modelId;
    Boolean isPrimary;
    String csvContent;
}
