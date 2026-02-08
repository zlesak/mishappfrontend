package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class representing validation result for a single question in a quiz.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizValidationQuestion {
    String questionText;
    Boolean isCorrect;
    Integer points;
    AbstractSubmissionData submission;
}
