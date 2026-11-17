package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.persistence.FullTextDocument;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The search has to narrow a listing, not widen it. These pin the query it builds, because the
 * difference between "every word" and "any word" is invisible until the collection is big enough
 * for a common word to match everything.
 */
class FullTextSearchServiceTest {

    private MongoTemplate mongoTemplate;
    private FullTextSearchService fullTextSearchService;

    @BeforeEach
    void setUp() {
        mongoTemplate = Mockito.mock(MongoTemplate.class);
        fullTextSearchService = new FullTextSearchService(mongoTemplate);
        Mockito.when(mongoTemplate.find(Mockito.any(), Mockito.eq(FullTextDocument.class)))
                .thenReturn(List.of(FullTextDocument.builder().externalId("ch-1").build()));
    }

    @Test
    void requiresEveryWordOfTheQueryToBePresent() {
        fullTextSearchService.search("Kapitola o mozku", FullTextDocument.FullTextType.CHAPTER);

        // One condition per word, all of which must hold: a chapter matching only "Kapitola" —
        // which is every chapter — must not come back.
        List<Document> words = wordConditions();
        assertThat(words).hasSize(3);
        assertThat(words.toString()).contains("Kapitola").contains("mozku");
        assertThat(words).allSatisfy(word -> assertThat(word).containsKey("text"));
    }

    @Test
    void treatsTheQueryLiterallyRatherThanAsAPattern() {
        fullTextSearchService.search("C++ (základy)", FullTextDocument.FullTextType.CHAPTER);

        // Unquoted, the brackets and pluses would be regex syntax and the query would either throw
        // or match something entirely different from what the user typed.
        assertThat(wordConditions().toString()).contains("\\Q").contains("\\E");
    }

    @Test
    void scopesTheSearchToTheRequestedKindOfEntity() {
        fullTextSearchService.search("mozek", FullTextDocument.FullTextType.CHAPTER);

        assertThat(capturedQuery().getQueryObject().get("type"))
                .isEqualTo(FullTextDocument.FullTextType.CHAPTER);
    }

    @Test
    void returnsNothingForAnEmptyQueryInsteadOfEverything() {
        assertThat(fullTextSearchService.search(null, FullTextDocument.FullTextType.CHAPTER)).isEmpty();
        assertThat(fullTextSearchService.search("   ", FullTextDocument.FullTextType.CHAPTER)).isEmpty();
        Mockito.verifyNoInteractions(mongoTemplate);
    }

    @SuppressWarnings("unchecked")
    private List<Document> wordConditions() {
        return (List<Document>) capturedQuery().getQueryObject().get("$and");
    }

    private Query capturedQuery() {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        Mockito.verify(mongoTemplate).find(captor.capture(), Mockito.eq(FullTextDocument.class));
        return captor.getValue();
    }
}
