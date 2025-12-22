package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * QuickQuizEntity Class - Represents a lightweight quiz entity containing list of chapters teh quiz belongs to.
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickQuizEntity extends AbstractEntity {
    String chapterId;
    Integer timeLimit;
}
