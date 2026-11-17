package cz.uhk.zlesak.threejslearningapp.domain.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Base class for filters used in various entities.
 * Contains common filtering fields such as name, creatorId, createdFrom, and createdTo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FilterBase {
    String Name;
    String CreatorId;
    /**
     * Author to filter by, matched on the recorded name rather than the id.
     *
     * <p>{@code CreatorId} is a Keycloak subject: it is not something a user can type, and offering it
     * in the interface would put other people's identifiers on screen.
     */
    String CreatorName;
    Instant CreatedFrom;
    Instant CreatedTo;
}
