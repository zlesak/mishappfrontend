package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * QuickChapterEntity Class - Represents a lightweight chapter entity containing list of sub-chapters.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickChapterEntity extends AbstractEntity {
    List<String> subChapters;
}
