package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * QuickQuizResult Class - Represents the result of a quiz taken by a user.
 * Extends AbstractEntity to inherit common entity properties.
 * @see AbstractEntity for inherited properties.
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickQuizResult extends AbstractEntity {
    String quizId;
    String userId;
    String name;
    String chapterName;
    Integer maxScore;
    Integer totalScore;
    Double percentage;
}
