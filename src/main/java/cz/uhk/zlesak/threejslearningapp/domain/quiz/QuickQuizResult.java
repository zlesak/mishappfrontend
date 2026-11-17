package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * The score of one quiz attempt, as shown in attempt listings.
 * {@link QuizValidationResult} adds the per-question feedback.
 *
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

    /** Who took the quiz; this is what scopes result listings to their owner. */
    String userId;

    String chapterName;

    Integer maxScore;

    Integer totalScore;

    Double percentage;
}
