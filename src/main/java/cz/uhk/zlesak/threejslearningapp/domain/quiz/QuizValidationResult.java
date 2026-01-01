package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Response object containing quiz validation results
 * Does not reveal correct answers when returned from the backend validation
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuizValidationResult extends QuickQuizResult {
    Integer totalScore;
    Integer maxScore;
    Double percentage;
    Map<String, Boolean> questionResults;
    Map<String, Integer> questionScores;
}

