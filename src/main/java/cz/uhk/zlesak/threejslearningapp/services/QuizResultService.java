package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.QuizResultApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Service for managing quizzes in the application.
 * Provides methods to create, update, delete, retrieve quizzes, and validate answers.
 */
@Slf4j
@Service
@Scope("prototype")
public class QuizResultService extends AbstractService<QuizValidationResult, QuickQuizResult, QuizResultFilter> {

    @Autowired
    public QuizResultService(QuizResultApiClient quizApiClient) {
        super(quizApiClient);
    }

    /**
     * Validates the create entity.
     *
     * @param createEntity Entity to validate
     * @return Validated entity
     * @throws RuntimeException if validation fails
     */
    @Override
    protected QuizValidationResult validateCreateEntity(QuizValidationResult createEntity) throws RuntimeException {
        return createEntity;
    }

    /**
     * Creates the final entity from the create entity.
     *
     * @param createEntity Entity to create
     * @return Final entity
     * @throws RuntimeException if creation fails
     */
    @Override
    protected QuizValidationResult createFinalEntity(QuizValidationResult createEntity) throws RuntimeException {
        return createEntity;
    }
}
