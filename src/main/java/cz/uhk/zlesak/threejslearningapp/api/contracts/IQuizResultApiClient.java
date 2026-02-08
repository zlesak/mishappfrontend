package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;

public interface IQuizResultApiClient {

    QuizValidationResult validateAnswers(QuizSubmissionRequest submissionRequest) throws Exception;
}
