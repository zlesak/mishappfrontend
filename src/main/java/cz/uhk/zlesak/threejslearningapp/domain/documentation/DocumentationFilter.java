package cz.uhk.zlesak.threejslearningapp.domain.documentation;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DocumentationFilter - Represents filtering criteria for querying documentation entries.
 * @see FilterBase for common filtering functionality.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class DocumentationFilter extends FilterBase {
    String SearchText;
    String Type;
}
