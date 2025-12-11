package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response object containing quiz validation results
 * Does not reveal correct answers when returned from the backend validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizValidationResult {
    Integer totalScore;
    Integer maxScore;
    Double percentage;
    Map<String, Boolean> questionResults;
    Map<String, Integer> questionScores;
}

