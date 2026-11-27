package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

/**
 * Optimistic locking on top of the {@code updated} timestamp every entity already carries.
 *
 * <p>An edit remembers the {@code updated} value of the version it was loaded from. Saving replaces
 * the document only if that value is still the stored one, and the check and the write are a single
 * Mongo operation, so two saves cannot both pass the check. The timestamp is used instead of a new
 * version field because every existing document already has it: a Spring Data {@code @Version}
 * field would be missing from them, and Spring treats an entity without a version as new and tries
 * to insert it.
 */
@Component
@RequiredArgsConstructor
public class VersionedWrites {

    private final MongoTemplate mongoTemplate;

    /**
     * Mongo stores dates with millisecond precision. Writing a finer timestamp would leave the
     * caller holding a value that no longer equals what was stored, and its next save would be
     * reported as a conflict with itself.
     *
     * @return the current time as Mongo will store it.
     */
    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * @param stored   {@code updated} of the stored version.
     * @param expected {@code updated} the edit was based on.
     * @return whether they denote the same version, at the precision Mongo keeps.
     */
    public static boolean sameVersion(Instant stored, Instant expected) {
        return Objects.equals(truncate(stored), truncate(expected));
    }

    /**
     * Replaces a document, but only if it is still the version the edit was based on.
     *
     * @param replacement     the new content; its id selects the document.
     * @param expectedUpdated {@code updated} of the version the edit was loaded from; {@code null}
     *                        matches only a document that has never had one (written before
     *                        timestamps existed).
     * @param type            entity type, used to map the id and the replacement.
     * @param collection      collection holding the document.
     * @return the version that was replaced, or {@code null} when nothing was written because the
     * document is gone or another save got there first.
     */
    public <T extends AbstractEntity> T replaceIfUnchanged(T replacement, Instant expectedUpdated,
                                                           Class<T> type, String collection) {
        Instant expected = truncate(expectedUpdated);
        Query query = new Query(Criteria.where("_id").is(replacement.getId())
                .and("updated").is(expected == null ? null : Date.from(expected)));
        return mongoTemplate.findAndReplace(query, replacement, FindAndReplaceOptions.empty(), type, collection);
    }

    private static Instant truncate(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MILLIS);
    }
}
