package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import org.springframework.data.mongodb.repository.MongoRepository;

/** CRUD access to graded quiz attempts. */
public interface QuizResultRepository extends MongoRepository<QuizValidationResult, String> {
}
