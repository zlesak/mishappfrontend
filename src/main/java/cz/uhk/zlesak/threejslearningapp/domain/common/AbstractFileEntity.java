package cz.uhk.zlesak.threejslearningapp.domain.common;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * AbstractFileEntity Class - Base class for file entities in the application. (Models and textures as of now)
 * Extends AbstractEntity and adds file-specific fields such as contentType and size.
 * @see AbstractEntity for common entity fields.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractFileEntity extends AbstractEntity {
    String contentType;
    Long size;
}
