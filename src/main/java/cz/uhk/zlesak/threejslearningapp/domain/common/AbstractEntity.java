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

    /**
     * Stable identity of the author, taken from the {@code sub} claim.
     * This is what ownership checks compare; it is never shown to a user.
     */
    String creatorId;

    /**
     * The author's username as it was at the time of writing, for display.
     * Recorded alongside {@link #creatorId} because the application has no directory of its own to
     * look a name up in later, and because a stale name is still more use than an opaque id.
     */
    String creatorName;

    Instant created;
    Instant updated;
    String description;

    /**
     * @return the author as a user should see them, or {@code null} when only the internal id is
     * known and there is therefore nothing meaningful to show.
     */
    public String displayCreator() {
        return creatorName == null || creatorName.isBlank() ? null : creatorName;
    }
}
