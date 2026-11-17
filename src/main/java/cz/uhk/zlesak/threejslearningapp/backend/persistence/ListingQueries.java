package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns the UI's filter and paging parameters into Mongo queries.
 * Blank filter values are treated as absent, and free-text values match names case-insensitively
 * anywhere in the name rather than requiring the exact string.
 */
@Component
@RequiredArgsConstructor
public class ListingQueries {

    /**
     * Fields the UI is allowed to sort by. Anything else is dropped rather than passed to Mongo,
     * so a stale or hand-crafted sort parameter cannot reach arbitrary document fields.
     */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("name", "created", "updated", "description", "creatorId", "creatorName", "totalScore",
                    "percentage");

    private final MongoTemplate mongoTemplate;

    /**
     * Builds the criteria shared by every listing: creator, name and creation window.
     *
     * @param filter filter supplied by the UI, may be {@code null}.
     * @return query carrying the matching criteria.
     */
    public Query baseQuery(FilterBase filter) {
        return baseQuery(filter, true);
    }

    /**
     * Builds the shared criteria without the name match, for callers that widen the search to more
     * than the name and supply their own criterion for it.
     *
     * @param filter filter supplied by the UI, may be {@code null}.
     * @return query carrying every criterion except the name.
     */
    public Query baseQueryWithoutName(FilterBase filter) {
        return baseQuery(filter, false);
    }

    private Query baseQuery(FilterBase filter, boolean includeName) {
        Query query = new Query();
        if (filter == null) {
            return query;
        }

        if (hasText(filter.getCreatorId())) {
            query.addCriteria(Criteria.where("creatorId").is(filter.getCreatorId()));
        }

        // By name, and matched loosely: the user types what they remember of the author, not an exact
        // account name and certainly not a subject id.
        if (hasText(filter.getCreatorName())) {
            query.addCriteria(Criteria.where("creatorName")
                    .regex(Pattern.quote(filter.getCreatorName().trim()), "i"));
        }

        String nameSearch = firstNonBlank(filter.getName(), searchTextOf(filter));
        if (includeName && nameSearch != null) {
            query.addCriteria(Criteria.where("name").regex(Pattern.quote(nameSearch), "i"));
        }

        if (filter.getCreatedFrom() != null && filter.getCreatedTo() != null) {
            query.addCriteria(Criteria.where("created").gte(filter.getCreatedFrom()).lte(filter.getCreatedTo()));
        } else if (filter.getCreatedFrom() != null) {
            query.addCriteria(Criteria.where("created").gte(filter.getCreatedFrom()));
        } else if (filter.getCreatedTo() != null) {
            query.addCriteria(Criteria.where("created").lte(filter.getCreatedTo()));
        }

        return query;
    }

    /**
     * Finds the ids of models whose name contains the given text.
     *
     * <p>A chapter document stores only model ids, so filtering chapters by the model they contain
     * means turning the name the user typed into a set of ids first.
     *
     * @param namePart part of a model name, matched case-insensitively; blank means no lookup.
     * @return ids of matching models, empty when nothing matches.
     */
    public List<String> modelIdsByName(String namePart) {
        if (!hasText(namePart)) {
            return List.of();
        }
        Query query = new Query(Criteria.where("name").regex(Pattern.quote(namePart.trim()), "i"));
        query.fields().include("_id");
        return mongoTemplate.find(query, IdOnly.class, MongoCollections.MODEL).stream()
                .map(IdOnly::id)
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }

    /** Just the identifier, for lookups whose only purpose is to feed an {@code in} criterion. */
    private record IdOnly(String id) {
    }

    /**
     * Counts matches and fetches one page of them.
     *
     * @param query          criteria to apply, mutated with paging by this call.
     * @param pageRequest    page number, size and sort requested by the UI.
     * @param type           document type to map results to.
     * @param collectionName collection to read from.
     * @param <T>            document type.
     * @return the requested page together with the total number of matches.
     */
    public <T> PageResult<T> page(Query query, PageRequest pageRequest, Class<T> type, String collectionName) {
        long total = mongoTemplate.count(Query.of(query), type, collectionName);
        List<T> elements = mongoTemplate.find(query.with(pageable(pageRequest)), type, collectionName);
        return new PageResult<>(elements, total, pageRequest.getPageNumber());
    }

    /**
     * Normalises the requested sort so only known document fields reach Mongo.
     *
     * @param pageRequest page request from the UI.
     * @return page request with a validated sort.
     */
    private PageRequest pageable(PageRequest pageRequest) {
        Sort sort = pageRequest.getSort();
        if (!sort.isSorted()) {
            return pageRequest;
        }

        Sort.Order order = sort.iterator().next();
        String field = normaliseSortField(order.getProperty());
        if (field == null) {
            return PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize());
        }
        return PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), order.getDirection(), field);
    }

    /**
     * Maps a UI field label onto a document field, accepting either capitalisation.
     *
     * @param property property name requested by the UI.
     * @return document field name, or {@code null} when the property is not sortable.
     */
    private String normaliseSortField(String property) {
        if (!hasText(property)) {
            return null;
        }
        String candidate = Character.toLowerCase(property.charAt(0)) + property.substring(1);
        return SORTABLE_FIELDS.stream()
                .filter(field -> field.equalsIgnoreCase(candidate))
                .findFirst()
                .orElse(null);
    }

    /**
     * Reads the free-text search value from the filter subtypes that carry one.
     *
     * @param filter filter to inspect.
     * @return search text, or {@code null} when the filter has none.
     */
    /**
     * @param filter filter supplied by the UI, may be {@code null}.
     * @return the free-text term the user typed, or {@code null} when they typed nothing.
     */
    public String searchTerm(FilterBase filter) {
        return filter == null ? null : firstNonBlank(filter.getName(), searchTextOf(filter));
    }

    private String searchTextOf(FilterBase filter) {
        if (filter instanceof ChapterFilter chapterFilter) {
            return chapterFilter.getSearchText();
        }
        if (filter instanceof ModelFilter modelFilter) {
            return modelFilter.getSearchText();
        }
        if (filter instanceof QuizFilter quizFilter) {
            return quizFilter.getSearchText();
        }
        return null;
    }

    private static String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        return hasText(second) ? second.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * @param value raw text.
     * @return the value lower-cased and trimmed, for storing normalised comparison keys.
     */
    public static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
