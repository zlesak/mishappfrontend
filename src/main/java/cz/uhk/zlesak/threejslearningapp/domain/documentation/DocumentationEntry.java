package cz.uhk.zlesak.threejslearningapp.domain.documentation;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single documentation entry (chapter, model or quiz).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentationEntry extends AbstractEntity {
    private String id;
    private String type;
    private String title;
    private String content;
    private List<String> roles;
}

