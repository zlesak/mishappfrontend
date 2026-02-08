package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IQuizResultApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * QuizApiClient provides connection to the backend service for managing quizzes.
 * It implements the IQuizApiClient interface and provides methods for creating, updating, deleting, and retrieving quizzes.
 * It uses RestClient for making HTTP requests to the backend service.
 */
@Component
public class QuizResultApiClient extends AbstractApiClient<QuizValidationResult, QuickQuizResult, QuizResultFilter> implements IQuizResultApiClient {
    private final String resultsEndpoint = IApiClient.getBaseUrl() + "quiz-result/";

    /**
     * Constructor for QuizApiClient.
     *
     * @param restClient RestClient for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     */
    @Autowired
    public QuizResultApiClient(RestClient restClient, ObjectMapper objectMapper) {
        super(restClient, objectMapper, "quiz-result/");
    }

    /**
     * Validates user's quiz answers.
     * Sends answers to backend for validation without exposing correct answers to frontend.
     *
     * @param submissionRequest User's submitted answers
     * @return Validation results with score and per-question correctness
     * @throws Exception if API call fails
     */
    @Override
    public QuizValidationResult validateAnswers(QuizSubmissionRequest submissionRequest) throws Exception {
        return sendPostRequest(resultsEndpoint + "validate-result", submissionRequest, QuizValidationResult.class, "Chyba při validaci odpovědí kvízu", submissionRequest.getQuizId(), null);
    }

    //region Overridden operations from AbstractApiClient

    /**
     * Gets the entity class for Quiz
     *
     * @return QuizEntity class
     */
    @Override
    protected Class<QuizValidationResult> getEntityClass() {
        return QuizValidationResult.class;
    }

    /**
     * Gets the quick entity class for Quiz
     *
     * @return QuickQuizEntity class
     */
    @Override
    protected Class<QuickQuizResult> getQuicEntityClass() {
        return QuickQuizResult.class;
    }
    //endregion
}
