package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request data class for validating quiz answers submitted by a user against the specified answers stored at the backend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionRequest {
    String quizId;
    List<AbstractSubmissionData> answers;
}

