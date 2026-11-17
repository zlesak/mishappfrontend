package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.MongoCollections;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.QuizResultRepository;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Owns graded quiz attempts: grading a submission and reading results back.
 * Students only ever see their own results; teachers see everyone's.
 */
@Service
@RequiredArgsConstructor
public class QuizResultBackendService {

    private static final String TEACHER_ROLE = "TEACHER";

    private final QuizResultRepository quizResultRepository;
    private final QuizBackendService quizBackendService;
    private final ChapterBackendService chapterBackendService;
    private final QuizAttemptService quizAttemptService;
    private final QuizGradingService quizGradingService;
    private final ListingQueries listingQueries;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLog auditLog;

    /**
     * Grades and stores a submitted quiz.
     *
     * @param submission the student's answers.
     * @return the stored result.
     */
    public QuizValidationResult submit(QuizSubmissionRequest submission) {
        if (submission == null || submission.getQuizId() == null || submission.getQuizId().isBlank()) {
            throw new BackendException.Validation("Odevzdání kvízu neobsahuje ID kvízu.");
        }

        String userId = currentUserProvider.requireUserId();
        // Checked first so a submission with no attempt behind it is refused before any work is
        // done, but only spent once the result is about to be written. Consuming it up front made
        // every later failure — a quiz that cannot be read, a grading error, a database hiccup —
        // destroy the attempt of a student who had submitted in time, leaving them unable to
        // submit at all. There is no transaction to lean on here: the store is a single Mongo
        // node, so the marker is removed immediately before the write rather than after it, which
        // keeps a single attempt from being graded twice.
        quizAttemptService.requireInFlight(submission.getQuizId());

        QuizEntity quiz = quizBackendService.load(submission.getQuizId(), true);
        QuizGradingService.Graded graded = quizGradingService.grade(quiz, submission.getAnswers());

        quizAttemptService.consume(submission.getQuizId());

        QuizValidationResult stored = quizResultRepository.save(QuizValidationResult.builder()
                .userId(userId)
                .quizId(quiz.getId())
                .name(quiz.getName())
                .chapterName(chapterNameOf(quiz))
                .totalScore(graded.totalScore())
                .maxScore(graded.maxScore())
                .percentage(graded.percentage())
                .questionResults(graded.results())
                .created(Instant.now())
                .build());
        auditLog.success(AuditLog.Action.SUBMIT, "quizResult", stored.getId(), quiz.getName());
        return stored;
    }

    /**
     * Records the chapter a quiz belonged to, so an attempt stays readable even after the quiz or
     * the chapter is renamed or removed.
     */
    private String chapterNameOf(QuizEntity quiz) {
        if (quiz.getChapterId() == null || quiz.getChapterId().isBlank()) {
            return null;
        }
        try {
            return chapterBackendService.require(quiz.getChapterId()).getName();
        } catch (BackendException e) {
            return null;
        }
    }

    /**
     * @param resultId id of the result.
     * @return the result.
     * @throws BackendException.NotFound  when no such result exists.
     * @throws BackendException.Forbidden when the result belongs to another student.
     */
    public QuizValidationResult require(String resultId) {
        if (resultId == null || resultId.isBlank()) {
            throw new BackendException.Validation("ID výsledku nesmí být prázdné.");
        }

        QuizValidationResult result = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new BackendException.NotFound("Výsledek kvízu s ID " + resultId + " nebyl nalezen."));

        String userId = currentUserProvider.requireUserId();
        if (!userId.equals(result.getUserId()) && !currentUserProvider.hasRole(TEACHER_ROLE)) {
            throw new BackendException.Forbidden("Výsledek kvízu jiného uživatele není přístupný.");
        }
        return result;
    }

    /**
     * @param filterParameters paging and filtering requested by the UI.
     * @return one page of results, restricted to the current user unless they are a teacher.
     */
    public PageResult<QuickQuizResult> list(FilterParameters<QuizResultFilter> filterParameters) {
        QuizResultFilter filter = filterParameters.getFilter();
        Query query = listingQueries.baseQuery(filter);

        if (filter != null && filter.getQuizId() != null && !filter.getQuizId().isBlank()) {
            query.addCriteria(Criteria.where("quizId").is(filter.getQuizId()));
        }
        if (!currentUserProvider.hasRole(TEACHER_ROLE)) {
            query.addCriteria(Criteria.where("userId").is(currentUserProvider.requireUserId()));
        }
        query.fields().exclude("questionResults");

        return listingQueries.page(query, filterParameters.getPageRequest(), QuickQuizResult.class,
                MongoCollections.QUIZ_RESULT);
    }
}
