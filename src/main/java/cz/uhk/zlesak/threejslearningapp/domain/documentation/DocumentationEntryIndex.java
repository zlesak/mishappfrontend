package cz.uhk.zlesak.threejslearningapp.domain.documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lightweight documentation projection for sidebar/list rendering without heavy content payload.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentationEntryIndex {
    private String id;
    private String type;
    private String title;
    private List<String> roles;
}

