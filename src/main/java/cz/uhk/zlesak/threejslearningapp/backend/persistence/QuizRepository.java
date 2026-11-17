package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** CRUD access to quizzes. */
public interface QuizRepository extends MongoRepository<QuizEntity, String> {

    /**
     * @param chapterId id of a chapter.
     * @return quizzes belonging to the chapter.
     */
    List<QuizEntity> findByChapterId(String chapterId);
}
