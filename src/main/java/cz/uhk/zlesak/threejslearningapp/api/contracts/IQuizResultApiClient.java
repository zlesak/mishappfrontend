package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;

/**
 * Quiz result data access, implemented either against the embedded backend or a remote one.
 */
public interface IQuizResultApiClient extends IApiClient<QuizValidationResult, QuickQuizResult, QuizResultFilter> {

    /**
     * Validates user's quiz answers against the stored correct answers.
     *
     * @param submissionRequest user's submitted answers.
     * @return validation result with score and per-question feedback.
     * @throws Exception if the submission cannot be graded.
     */
    QuizValidationResult validateAnswers(QuizSubmissionRequest submissionRequest) throws Exception;
}
