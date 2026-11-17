package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;

import java.util.List;

/**
 * A chapter without its content: enough to list it and to show which models it uses.
 * {@link ChapterEntity} adds the editor content a detail screen needs.
 *
 * <p>Only {@link #modelIds} is stored. The models are read from their own collection when a chapter
 * is loaded, so renaming or replacing a model is immediately reflected everywhere it is used.
 *
 * @see AbstractEntity for common entity properties.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickChapterEntity extends AbstractEntity {

    /** The models this chapter uses, in display order; the main model comes first. */
    List<String> modelIds;

    /** Models resolved from {@link #modelIds} when the chapter is read. */
    @Transient
    List<QuickModelEntity> models;

    /** Derived from the content for the navigation panel; never stored. */
    @Transient
    List<String> subChapters;
}
