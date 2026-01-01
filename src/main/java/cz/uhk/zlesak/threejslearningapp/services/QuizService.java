package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.QuizApiClient;
import cz.uhk.zlesak.threejslearningapp.api.clients.QuizResultApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing quizzes in the application.
 * Provides methods to create, update, delete, retrieve quizzes, and validate answers.
 */
@Slf4j
@Service
@Scope("prototype")
public class QuizService extends AbstractService<QuizEntity, QuickQuizEntity, QuizFilter> {
    private final QuizApiClient quizApiClient;
    private final QuizResultApiClient quizResultApiClient;
    private final List<AbstractQuestionData> questions = new ArrayList<>();
    private final List<AbstractAnswerData> answers = new ArrayList<>();

    @Autowired
    public QuizService(QuizApiClient quizApiClient, QuizResultApiClient quizResultApiClient) {
        super(quizApiClient);
        this.quizApiClient = quizApiClient;
        this.quizResultApiClient = quizResultApiClient;
    }

    /**
     * Clears the current list of questions and answers.
     */
    public void clearQuestionsAndAnswers() {
        this.questions.clear();
        this.answers.clear();
    }

    /**
     * Validates user's answers for a quiz.
     *
     * @param quizId      Quiz ID
     * @param answers List of user's answer submissions
     * @return Validation result with score and feedback
     * @throws Exception if API call fails
     */
    public QuizValidationResult validateAnswers(String quizId, List<AbstractSubmissionData> answers) throws Exception {
        QuizSubmissionRequest request = new QuizSubmissionRequest(quizId, answers);
        return quizResultApiClient.validateAnswers(request);
    }

    /**
     * Gets quiz data for student view (without correct answers).
     *
     * @param quizId Quiz ID
     * @return Quiz entity for student
     */
    public QuizEntity getQuizForStudent(String quizId){

        try {
            return quizApiClient.readQuizStudent(quizId);
        } catch (Exception e) {
            log.error("Nepodařilo se naříst kvíz: {}", String.valueOf(e));
            throw new ApplicationContextException("Nepodařilo se naříst kvíz");
        }
    }

    /**
     * Adds a question to the current quiz being built.
     *
     * @param question Question to add
     */
    public void addQuestion(AbstractQuestionData question) {
        this.questions.add(question);
    }

    /**
     * Adds an answer to the current quiz being built.
     *
     * @param answer Answer to add
     */
    public void addAnswer(AbstractAnswerData answer) {
        this.answers.add(answer);
    }

    /**
     * Validates the create entity.
     *
     * @param createEntity Entity to validate
     * @return Validated entity
     * @throws RuntimeException if validation fails
     */
    @Override
    protected QuizEntity validateCreateEntity(QuizEntity createEntity) throws RuntimeException {

        if (createEntity.getName() == null || createEntity.getName().isEmpty()) {
            throw new ApplicationContextException("Název kvízu nesmí být prázdný.");
        }
        if (createEntity.getDescription() == null || createEntity.getDescription().isEmpty()) {
            throw new ApplicationContextException("Kvíz musí obsahovat popis.");
        }
        if (createEntity.getTimeLimit() == null || createEntity.getTimeLimit() < 0) {
            throw new ApplicationContextException("Čas kvízu nesmí být záporné číslo.");
        }
        if (questions.isEmpty()) {
            throw new ApplicationContextException("Kvíz musí obsahovat alespoň jednu otázku.");
        }
        if (answers.isEmpty() || answers.size() != questions.size()) {
            throw new ApplicationContextException("Kvíz musí obsahovat odpovědi na všechny otázky.");
        }
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
    protected QuizEntity createFinalEntity(QuizEntity createEntity) throws RuntimeException {
        return createEntity.toBuilder()
                .questions(questions)
                .answers(answers)
                .created(Instant.now())
                .build();
    }
}
