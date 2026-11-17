package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ListingQueriesTest {

    private final ListingQueries listingQueries = new ListingQueries(Mockito.mock(MongoTemplate.class));

    @Test
    void ignoresAFilterThatTheUserLeftEmpty() {
        Query query = listingQueries.baseQuery(QuizResultFilter.builder().Name("").CreatorId("  ").build());

        assertThat(query.getQueryObject()).isEmpty();
        assertThat(listingQueries.baseQuery(null).getQueryObject()).isEmpty();
    }

    @Test
    void matchesNamesAnywhereAndRegardlessOfCase() {
        Query query = listingQueries.baseQuery(ChapterFilter.builder().SearchText("mozek").build());

        assertThat(query.getQueryObject().toJson()).contains("name").contains("mozek");
    }

    @Test
    void treatsCharactersWithMeaningInPatternsAsPlainText() {
        Query query = listingQueries.baseQuery(ChapterFilter.builder().SearchText("a.b*c").build());

        assertThat(query.getQueryObject().toJson()).contains("\\\\Qa.b*c\\\\E");
    }

    @Test
    void narrowsByCreatorAndCreationWindow() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T00:00:00Z");
        FilterBase filter = QuizResultFilter.builder().CreatorId("alice").CreatedFrom(from).CreatedTo(to).build();

        org.bson.Document criteria = listingQueries.baseQuery(filter).getQueryObject();

        assertThat(criteria).containsKeys("creatorId", "created");
        assertThat(criteria.get("created", org.bson.Document.class)).containsKeys("$gte", "$lte");
    }

    @Test
    void appliesOnlyTheEndOfAnOpenCreationWindow() {
        org.bson.Document criteria = listingQueries
                .baseQuery(QuizResultFilter.builder().CreatedTo(Instant.parse("2024-12-31T00:00:00Z")).build())
                .getQueryObject();

        assertThat(criteria.get("created", org.bson.Document.class))
                .containsKey("$lte").doesNotContainKey("$gte");
    }

    @Test
    void reportsTheFreeTextTermTheUserTyped() {
        assertThat(listingQueries.searchTerm(ChapterFilter.builder().SearchText("  mozek  ").build())).isEqualTo("mozek");
        assertThat(listingQueries.searchTerm(ChapterFilter.builder().SearchText("   ").build())).isNull();
        assertThat(listingQueries.searchTerm(null)).isNull();
    }
}
