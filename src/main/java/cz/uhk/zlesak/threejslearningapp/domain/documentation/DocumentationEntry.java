package cz.uhk.zlesak.threejslearningapp.domain.documentation;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single documentation entry (chapter, model or quiz).
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentationEntry extends AbstractEntity {
    private String id;
    private String type;
    private String title;
    private String content;
    private List<String> roles;

    public DocumentationEntryIndex toIndex() {
        return new DocumentationEntryIndex(id, type, title, roles == null ? List.of() : List.copyOf(roles));
    }
}

