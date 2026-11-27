package cz.uhk.zlesak.threejslearningapp.testsupport;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import org.bson.Document;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * One Mongo collection held in memory, answering {@code findAndReplace} the way Mongo does: the
 * filter is applied to the stored document and the replacement happens only when every condition in
 * it holds. A service that leaves the version out of its filter therefore overwrites here exactly as
 * it would against a real database, which is what lets the conflict tests fail on the bug.
 *
 * @param <T> entity type stored in the collection.
 */
public final class VersionedCollectionFake<T extends AbstractEntity> {

    private final Map<String, T> documents = new HashMap<>();
    private final UnaryOperator<T> copier;

    /**
     * Routes {@code findAndReplace} for the given type and collection on a mocked template here.
     */
    public VersionedCollectionFake(MongoTemplate mongoTemplate, Class<T> type, String collection) {
        this(mongoTemplate, type, collection, UnaryOperator.identity());
    }

    /**
     * @param copier makes the copy a read hands out. Needed for services that change the entity
     *               they loaded and write it back: a database read is a copy, and handing out the
     *               stored instance would let the change reach the "database" before the write.
     */
    public VersionedCollectionFake(MongoTemplate mongoTemplate, Class<T> type, String collection,
                                   UnaryOperator<T> copier) {
        this.copier = copier;
        Mockito.when(mongoTemplate.findAndReplace(Mockito.any(Query.class), Mockito.any(),
                        Mockito.any(FindAndReplaceOptions.class), Mockito.eq(type), Mockito.eq(collection)))
                .thenAnswer(call -> findAndReplace(call.getArgument(0), type.cast(call.getArgument(1))));
    }

    /** Stores a document as if it had been written earlier. */
    public void put(T document) {
        documents.put(document.getId(), document);
    }

    /** @return the stored document, if any. */
    public Optional<T> find(String id) {
        return Optional.ofNullable(documents.get(id)).map(copier);
    }

    /** @return the stored document. */
    public T get(String id) {
        return find(id).orElseThrow();
    }

    private T findAndReplace(Query query, T replacement) {
        Document filter = query.getQueryObject();
        T stored = documents.get(String.valueOf(filter.get("_id")));
        if (stored == null) {
            return null;
        }
        if (filter.containsKey("updated")) {
            Date storedUpdated = stored.getUpdated() == null ? null : Date.from(stored.getUpdated());
            if (!Objects.equals(storedUpdated, filter.get("updated"))) {
                return null;
            }
        }
        documents.put(stored.getId(), replacement);
        return stored;
    }
}
