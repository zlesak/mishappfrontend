package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

/** CRUD access to in-flight quiz attempts. */
public interface QuizAttemptRepository extends MongoRepository<QuizAttemptDocument, String> {
}
