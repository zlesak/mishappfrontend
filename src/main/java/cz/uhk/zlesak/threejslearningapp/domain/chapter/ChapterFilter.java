package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * ChapterFilter Class - Represents filtering criteria for querying chapters.
 * @see FilterBase for common filtering functionality.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class ChapterFilter extends FilterBase {
    String SearchText;
    /**
     * Name of a model the chapter has to contain.
     *
     * <p>Chapters store only model ids, so this is resolved to ids before the chapter query runs.
     */
    String ModelName;
}
