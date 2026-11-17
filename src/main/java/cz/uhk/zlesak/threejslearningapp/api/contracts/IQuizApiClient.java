package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizFilter;

/**
 * Quiz data access, implemented either against the embedded backend or a remote one.
 */
public interface IQuizApiClient extends IApiClient<QuizEntity, QuickQuizEntity, QuizFilter> {

    /**
     * Reads a quiz for a student and starts their attempt. The correct answers are not included.
     *
     * @param quizId Quiz ID
     * @return Quiz entity without answers
     * @throws Exception if the quiz cannot be read
     */
    QuizEntity readQuizStudent(String quizId) throws Exception;

    /**
     * Reads all quiz data including correct answers for admin/teacher view.
     *
     * @param quizId Quiz ID.
     * @return Quiz entity with all answers.
     * @throws Exception if the quiz cannot be read.
     */
    QuizEntity readAll(String quizId) throws Exception;
}
