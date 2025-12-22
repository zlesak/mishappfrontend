package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;

/**
 * Interface for Quiz API Client
 * Defines methods for reading quizzes as a student and validating quiz submissions.
 */
public interface IQuizApiClient {

    QuizEntity readQuizStudent(String quizId) throws Exception;

    QuizValidationResult validateAnswers(QuizSubmissionRequest submissionRequest) throws Exception;
}
