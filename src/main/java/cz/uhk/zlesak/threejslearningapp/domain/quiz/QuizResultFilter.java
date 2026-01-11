package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * QuizResultFilter Class - Represents filter criteria for querying quiz results.
 * Extends FilterBase to inherit common filtering properties.
 * @see FilterBase for inherited properties.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class QuizResultFilter extends FilterBase {
    String chapterId;
    String quizId;
}
