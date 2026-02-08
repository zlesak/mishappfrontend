package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * QuizFilter Class - Represents filter criteria for querying quizzes.
 * Extends FilterBase to inherit common filtering properties.
 * @see FilterBase for inherited properties.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class QuizFilter extends FilterBase {
    String SearchText;
}
