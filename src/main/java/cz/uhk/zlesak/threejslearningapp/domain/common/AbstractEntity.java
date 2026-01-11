package cz.uhk.zlesak.threejslearningapp.domain.common;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * AbstractEntity Class - Base class for all entities in the application.
 * Contains common fields such as id, name, creatorId, created timestamp, updated timestamp, and description.
 * Implements IEntity interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
public abstract class AbstractEntity implements IEntity {
    String id;
    String name;
    String creatorId;
    Instant created;
    Instant updated;
    String description;
}
