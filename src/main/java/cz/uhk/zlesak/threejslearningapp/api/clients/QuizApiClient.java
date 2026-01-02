package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IQuizApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * QuizApiClient provides connection to the backend service for managing quizzes.
 * It implements the IQuizApiClient interface and provides methods for creating, updating, deleting, and retrieving quizzes.
 * It uses RestTemplate for making HTTP requests to the backend service.
 */
@Component
public class QuizApiClient extends AbstractApiClient<QuizEntity, QuickQuizEntity, QuizFilter> implements IQuizApiClient {

    /**
     * Constructor for QuizApiClient.
     *
     * @param restTemplate RestTemplate for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     */
    @Autowired
    public QuizApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper, "quiz/");
    }

    /**
     * Gets a quiz without answers for student.
     *
     * @param quizId Quiz ID
     * @return Quiz entity without answers
     * @throws Exception if API call fails
     */
    @Override
    public QuizEntity readQuizStudent(String quizId) throws Exception {
        return sendGetRequest(baseUrl + quizId + "/questions", getEntityClass(), "Chyba při získávání entity dle ID", quizId, "startQuiz", "true");
    }

    @Override
    public QuizEntity readAll(String quizId) throws Exception {
        return sendGetRequest(baseUrl + quizId + "/all", getEntityClass(), "Chyba při získávání entity dle ID", quizId, "startQuiz", "true");
    }

    //region Overridden operations from AbstractApiClient
    /**
     * Gets the entity class for Quiz
     *
     * @return QuizEntity class
     */
    @Override
    protected Class<QuizEntity> getEntityClass() {
        return QuizEntity.class;
    }

    /**
     * Gets the quick entity class for Quiz
     *
     * @return QuickQuizEntity class
     */
    @Override
    protected Class<QuickQuizEntity> getQuicEntityClass() {
        return QuickQuizEntity.class;
    }
    //endregion
}
