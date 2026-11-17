package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Searchable projection of an indexed entity. A Mongo text index on {@link #text} backs the
 * full-text search; {@link #externalId} points back at the indexed document.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = MongoCollections.FULL_TEXT)
public class FullTextDocument {

    @Id
    private String id;
    @Indexed(unique = true)
    private String externalId;
    private FullTextType type;
    private String text;

    /** Kinds of entity that can be full-text indexed. */
    public enum FullTextType {
        CHAPTER
    }
}
