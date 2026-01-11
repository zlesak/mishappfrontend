package cz.uhk.zlesak.threejslearningapp.domain.file;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractFileEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * QuickFileEntity Class - Represents a quick file entity with backend endpoint information.
 * Extends AbstractFileEntity to include file-specific fields.
 * @see AbstractFileEntity for common file entity fields.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickFileEntity extends AbstractFileEntity {
    String backendEndpoint;
}
