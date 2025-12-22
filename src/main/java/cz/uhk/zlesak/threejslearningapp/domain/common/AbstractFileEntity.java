package cz.uhk.zlesak.threejslearningapp.domain.common;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * AbstractFileEntity Class - Base class for file entities in the application. (Models and textures as of now)
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
