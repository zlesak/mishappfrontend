package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;

/**
 * Interface for quiz result API client operations.
 */
public interface IQuizResultApiClient {

    /**
     * Validates user's quiz answers against the stored correct answers.
     *
     * @param submissionRequest user's submitted answers.
     * @return validation result with score and per-question feedback.
     * @throws Exception if the API call fails.
     */
    QuizValidationResult validateAnswers(QuizSubmissionRequest submissionRequest) throws Exception;
}
