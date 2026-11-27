package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.MongoCollections;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.QuizRepository;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.VersionedWrites;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Owns quizzes. The correct answers are left out unless the caller is allowed to see them, so a
 * student loading a quiz never receives the answer key.
 */
@Service
@RequiredArgsConstructor
public class QuizBackendService {

    private final QuizRepository quizRepository;
    private final MongoTemplate mongoTemplate;
    private final QuizAuthoringValidator authoringValidator;
    private final ChapterBackendService chapterBackendService;
    private final ListingQueries listingQueries;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLog auditLog;
    private final VersionedWrites versionedWrites;

    /**
     * Stores a new quiz.
     *
     * @param quiz quiz to store.
     * @return the stored quiz.
     */
    @PreAuthorize("hasRole('CREATE_QUIZ')")
    public QuizEntity create(QuizEntity quiz) {
        validate(quiz);
        quiz.setId(null);
        quiz.setCreatorId(currentUserProvider.requireUserId());
        quiz.setCreatorName(currentUserProvider.currentUserName());
        Instant now = VersionedWrites.now();
        quiz.setCreated(quiz.getCreated() != null ? quiz.getCreated() : now);
        quiz.setUpdated(now);
        QuizEntity saved = quizRepository.save(quiz);
        auditLog.success(AuditLog.Action.CREATE, "quiz", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * Updates an existing quiz, keeping its original author and creation time.
     *
     * <p>The save only goes through if nobody else has saved the quiz since the editor loaded it:
     * {@code quiz.updated} must still be the stored value. Otherwise nothing is written and the
     * caller gets the stored version, answers included, to compare with.
     *
     * @param quiz quiz carrying the new content; its id must be set and its {@code updated} must be
     *             the value of the version the edit started from.
     * @return the stored quiz.
     * @throws BackendException.Conflict when the quiz was saved by someone else in the meantime.
     */
    @PreAuthorize("hasRole('CREATE_QUIZ')")
    public QuizEntity update(QuizEntity quiz) {
        if (quiz.getId() == null || quiz.getId().isBlank()) {
            throw new BackendException.Validation("Kvíz nelze aktualizovat bez ID.");
        }
        QuizEntity existing = load(quiz.getId(), true);
        validate(quiz);
        currentUserProvider.requireAuthorOrTeacher(existing.getCreatorId(),
                "Kvíz může upravit pouze jeho autor.");

        Instant basedOn = quiz.getUpdated();
        quiz.setCreatorId(existing.getCreatorId() != null ? existing.getCreatorId() : currentUserProvider.requireUserId());
        quiz.setCreatorName(existing.getCreatorName() != null
                ? existing.getCreatorName()
                : currentUserProvider.currentUserName());
        quiz.setCreated(existing.getCreated());
        quiz.setUpdated(VersionedWrites.now());

        if (versionedWrites.replaceIfUnchanged(quiz, basedOn, QuizEntity.class, MongoCollections.QUIZ) == null) {
            quiz.setUpdated(basedOn);
            auditLog.failure(AuditLog.Action.UPDATE, "quiz", quiz.getId(), "conflict");
            throw new BackendException.Conflict(
                    "Kvíz mezitím uložil někdo jiný. Vaše změny nebyly uloženy.", load(quiz.getId(), true));
        }
        auditLog.success(AuditLog.Action.UPDATE, "quiz", quiz.getId(), quiz.getName());
        return quiz;
    }

    /**
     * Reads a quiz without its correct answers. This is the only read the UI performs on behalf of
     * a student.
     *
     * @param quizId id of the quiz.
     * @return the quiz, with {@code answers} left out.
     * @throws BackendException.NotFound when no such quiz exists.
     */
    public QuizEntity require(String quizId) {
        return load(quizId, false);
    }

    /**
     * Reads a quiz together with its answer key, for authoring.
     * Guarded here and not only by the view's route annotation: the answer key is the one piece of
     * quiz data that must never reach a student, so the UI is not the layer that decides it.
     *
     * @param quizId id of the quiz.
     * @return the quiz, including {@code answers}.
     * @throws BackendException.NotFound when no such quiz exists.
     */
    @PreAuthorize("hasRole('CREATE_QUIZ')")
    public QuizEntity requireWithAnswers(String quizId) {
        return load(quizId, true);
    }

    /**
     * Reads a quiz, optionally with its answer key.
     * Package-private and unguarded on purpose: grading needs the answers while running as the
     * student who submitted, so the check cannot live here. Every caller outside this package goes
     * through {@link #require(String)} or {@link #requireWithAnswers(String)}.
     *
     * @param quizId      id of the quiz.
     * @param showAnswers whether the correct answers should be included.
     * @return the quiz.
     * @throws BackendException.NotFound when no such quiz exists.
     */
    QuizEntity load(String quizId, boolean showAnswers) {
        if (quizId == null || quizId.isBlank()) {
            throw new BackendException.Validation("ID kvízu nesmí být prázdné.");
        }

        Query query = new Query(Criteria.where("_id").is(quizId));
        if (!showAnswers) {
            query.fields().exclude("answers");
        }

        QuizEntity quiz = mongoTemplate.findOne(query, QuizEntity.class, MongoCollections.QUIZ);
        if (quiz == null) {
            throw new BackendException.NotFound("Kvíz s ID " + quizId + " nebyl nalezen.");
        }
        return quiz;
    }

    /**
     * Removes a quiz.
     * The quiz is read first so that deleting one that is not there is reported as such rather
     * than passing silently, and so the audit trail records the name that was removed instead of
     * claiming a success for a request that changed nothing.
     *
     * @param quizId id of the quiz.
     * @throws BackendException.NotFound when no such quiz exists.
     */
    @PreAuthorize("hasRole('CREATE_QUIZ')")
    public void delete(String quizId) {
        QuizEntity existing = load(quizId, false);
        currentUserProvider.requireAuthorOrTeacher(existing.getCreatorId(),
                "Kvíz může smazat pouze jeho autor.");

        quizRepository.deleteById(existing.getId());
        auditLog.success(AuditLog.Action.DELETE, "quiz", existing.getId(), existing.getName());
    }

    /**
     * @param filterParameters paging and filtering requested by the UI.
     * @return one page of quizzes, without their questions and answers.
     */
    public PageResult<QuickQuizEntity> list(FilterParameters<QuizFilter> filterParameters) {
        Query query = listingQueries.baseQuery(filterParameters.getFilter());
        query.fields().exclude("questions").exclude("answers");
        return listingQueries.page(query, filterParameters.getPageRequest(), QuickQuizEntity.class,
                MongoCollections.QUIZ);
    }

    /**
     * Clears the chapter reference of every quiz that belonged to a chapter being removed.
     * A quiz without a chapter is valid and stays editable; one still pointing at a deleted chapter
     * could neither be opened for editing nor saved, because both paths resolve the chapter first.
     *
     * @param chapterId id of the chapter being removed.
     * @return how many quizzes were detached.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public long detachFromChapter(String chapterId) {
        if (chapterId == null || chapterId.isBlank()) {
            return 0;
        }
        return mongoTemplate.updateMulti(
                new Query(Criteria.where("chapterId").is(chapterId)),
                new Update().unset("chapterId"),
                QuizEntity.class,
                MongoCollections.QUIZ).getModifiedCount();
    }

    /**
     * @param chapterId id of a chapter.
     * @return the quizzes belonging to that chapter, for the chapter detail screen.
     */
    public List<QuizEntity> byChapter(String chapterId) {
        if (chapterId == null || chapterId.isBlank()) {
            return List.of();
        }
        return quizRepository.findByChapterId(chapterId);
    }

    private void validate(QuizEntity quiz) {
        if (quiz.getName() == null || quiz.getName().isBlank()) {
            throw new BackendException.Validation("Název kvízu nesmí být prázdný.");
        }
        if (quiz.getTimeLimit() != null && quiz.getTimeLimit() < 0) {
            throw new BackendException.Validation("Časový limit kvízu nesmí být záporný.");
        }
        if (quiz.getChapterId() != null && !quiz.getChapterId().isBlank()) {
            // Recorded while the chapter is being checked anyway, so listings can name the chapter
            // instead of showing a fragment of its id.
            quiz.setChapterName(chapterBackendService.require(quiz.getChapterId()).getName());
        } else {
            quiz.setChapterName(null);
        }
        authoringValidator.validate(quiz.getQuestions(), quiz.getAnswers());
    }
}
