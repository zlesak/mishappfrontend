package cz.uhk.zlesak.threejslearningapp.backend.persistence;

/**
 * Names of the MongoDB collections. Each collection holds one kind of entity, and every entity is
 * stored whole, so a read is a single lookup by id.
 */
public final class MongoCollections {

    public static final String CHAPTER = "chapters";
    public static final String QUIZ = "quiz";
    public static final String QUIZ_RESULT = "quizResults";
    public static final String MODEL = "models";
    public static final String FULL_TEXT = "fulltext";
    public static final String QUIZ_ATTEMPT = "quizAttempts";

    private MongoCollections() {
    }
}
