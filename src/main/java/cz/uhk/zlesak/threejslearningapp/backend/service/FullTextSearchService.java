package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.persistence.FullTextDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Keeps a searchable text projection of chapters and answers keyword queries against it.
 * Matching is literal and case-insensitive rather than stemmed, which is what makes it behave the
 * same for Czech as for English.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FullTextSearchService {

    private final MongoTemplate mongoTemplate;

    /**
     * Indexes an entity, replacing any previous entry for it.
     *
     * @param externalId id of the indexed entity.
     * @param type       kind of entity.
     * @param texts      texts to index; blank values are skipped.
     */
    public void index(String externalId, FullTextDocument.FullTextType type, String... texts) {
        String combined = combine(texts);
        if (externalId == null || combined.isBlank()) {
            return;
        }

        mongoTemplate.upsert(
                new Query(Criteria.where("externalId").is(externalId)),
                new Update().set("text", combined).set("type", type),
                FullTextDocument.class);
    }

    /**
     * Drops an entity from the index, so deleted entities stop appearing in search results.
     *
     * @param externalId id of the entity.
     */
    public void remove(String externalId) {
        if (externalId == null) {
            return;
        }
        mongoTemplate.remove(new Query(Criteria.where("externalId").is(externalId)), FullTextDocument.class);
    }

    /**
     * Finds entities whose indexed text contains <em>every</em> word the user typed.
     *
     * <p>Deliberately not a Mongo {@code $text} query: that one matches any single word of the
     * phrase, so searching for "Kapitola o mozku" would return every chapter with "kapitola"
     * anywhere in it — which, without relevance ranking, is the whole collection. Requiring all
     * words is what makes the search narrow the listing instead of widening it.
     *
     * @param keyword text typed by the user.
     * @param type    kind of entity to search.
     * @return ids of the matching entities.
     */
    public List<String> search(String keyword, FullTextDocument.FullTextType type) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        Criteria[] words = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(word -> !word.isBlank())
                .map(word -> Criteria.where("text").regex(Pattern.quote(word), "i"))
                .toArray(Criteria[]::new);
        if (words.length == 0) {
            return List.of();
        }

        Query query = new Query(new Criteria().andOperator(words));
        query.addCriteria(Criteria.where("type").is(type));

        return mongoTemplate.find(query, FullTextDocument.class).stream()
                .map(FullTextDocument::getExternalId)
                .filter(Objects::nonNull)
                .toList();
    }

    private String combine(String... texts) {
        if (texts == null) {
            return "";
        }
        return String.join(" ", java.util.Arrays.stream(texts)
                .filter(text -> text != null && !text.isBlank())
                .toList());
    }
}
