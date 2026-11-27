package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.MongoCollections;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.QuizRepository;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.VersionedWrites;
import cz.uhk.zlesak.threejslearningapp.testsupport.VersionedCollectionFake;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonInt64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the two properties of quiz reads that are not visible from the outside: which read hands
 * back the answer key, and what happens to a quiz when its chapter is removed.
 */
class QuizBackendServiceTest {

    private MongoTemplate mongoTemplate;
    private QuizRepository quizRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditLog auditLog;
    private QuizBackendService quizBackendService;

    @BeforeEach
    void setUp() {
        mongoTemplate = Mockito.mock(MongoTemplate.class);
        quizRepository = Mockito.mock(QuizRepository.class);
        currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        auditLog = Mockito.mock(AuditLog.class);
        quizBackendService = new QuizBackendService(
                quizRepository,
                mongoTemplate,
                Mockito.mock(QuizAuthoringValidator.class),
                Mockito.mock(ChapterBackendService.class),
                Mockito.mock(ListingQueries.class),
                currentUserProvider,
                auditLog,
                new VersionedWrites(mongoTemplate));

        Mockito.when(mongoTemplate.findOne(Mockito.any(), Mockito.eq(QuizEntity.class), Mockito.anyString()))
                .thenReturn(QuizEntity.builder().name("Kvíz").build());
    }

    @Test
    void theStudentReadLeavesTheAnswerKeyOut() {
        quizBackendService.require("quiz-1");

        assertThat(capturedQuery().getFieldsObject().toJson()).contains("\"answers\": 0");
    }

    @Test
    void theAuthoringReadKeepsTheAnswerKey() {
        quizBackendService.requireWithAnswers("quiz-1");

        assertThat(capturedQuery().getFieldsObject().isEmpty()).isTrue();
    }

    @Test
    void rejectsABlankQuizId() {
        assertThatThrownBy(() -> quizBackendService.require(" "))
                .isInstanceOf(BackendException.Validation.class);
    }

    @Test
    void reportsAMissingQuizAsNotFound() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(), Mockito.eq(QuizEntity.class), Mockito.anyString()))
                .thenReturn(null);

        assertThatThrownBy(() -> quizBackendService.require("gone"))
                .isInstanceOf(BackendException.NotFound.class);
    }

    @Test
    void detachingFromAChapterClearsTheReferenceOnEveryQuizOfThatChapter() {
        Mockito.when(mongoTemplate.updateMulti(Mockito.any(), Mockito.any(Update.class),
                        Mockito.eq(QuizEntity.class), Mockito.anyString()))
                .thenReturn(UpdateResult.acknowledged(2, 2L, new BsonInt64(0)));

        assertThat(quizBackendService.detachFromChapter("ch-1")).isEqualTo(2);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        Mockito.verify(mongoTemplate).updateMulti(query.capture(), update.capture(),
                Mockito.eq(QuizEntity.class), Mockito.eq(MongoCollections.QUIZ));

        assertThat(query.getValue().getQueryObject().toJson()).contains("ch-1");
        assertThat(update.getValue().getUpdateObject().toJson()).contains("$unset").contains("chapterId");
    }

    @Test
    void detachingIgnoresABlankChapterId() {
        assertThat(quizBackendService.detachFromChapter(" ")).isZero();
        Mockito.verifyNoInteractions(mongoTemplate);
    }

    @Test
    void deletingAQuizChecksWhoAuthoredIt() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(), Mockito.eq(QuizEntity.class), Mockito.anyString()))
                .thenReturn(QuizEntity.builder().id("quiz-1").name("Kvíz").creatorId("alice").build());

        quizBackendService.delete("quiz-1");

        Mockito.verify(currentUserProvider).requireAuthorOrTeacher(Mockito.eq("alice"), Mockito.anyString());
        Mockito.verify(quizRepository).deleteById("quiz-1");
    }

    @Test
    void deletingAQuizThatIsNotThereIsReportedRatherThanAuditedAsASuccess() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(), Mockito.eq(QuizEntity.class), Mockito.anyString()))
                .thenReturn(null);

        assertThatThrownBy(() -> quizBackendService.delete("gone"))
                .isInstanceOf(BackendException.NotFound.class);

        Mockito.verify(quizRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verifyNoInteractions(auditLog);
    }

    @Test
    void deletingAQuizRecordsTheNameThatWasRemoved() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(), Mockito.eq(QuizEntity.class), Mockito.anyString()))
                .thenReturn(QuizEntity.builder().id("quiz-1").name("Kvíz o mozku").build());

        quizBackendService.delete("quiz-1");

        Mockito.verify(auditLog).success(AuditLog.Action.DELETE, "quiz", "quiz-1", "Kvíz o mozku");
    }

    @Test
    void aSaveBasedOnTheStoredVersionGoesThrough() {
        VersionedCollectionFake<QuizEntity> quizzes = storedQuiz();

        QuizEntity saved = quizBackendService.update(edit("Kvíz upraven", T1));

        assertThat(quizzes.get("quiz-1").getName()).isEqualTo("Kvíz upraven");
        assertThat(saved.getUpdated()).isAfter(T1);
        Mockito.verify(quizRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void aStaleSaveIsRejectedWithTheStoredVersionAndReportsNoSuccess() {
        VersionedCollectionFake<QuizEntity> quizzes = storedQuiz();
        quizBackendService.update(edit("Okno A", T1));
        Mockito.clearInvocations(auditLog);

        assertThatThrownBy(() -> quizBackendService.update(edit("Okno B", T1)))
                .isInstanceOfSatisfying(BackendException.Conflict.class, conflict -> {
                    assertThat(conflict.getCurrent().getName()).isEqualTo("Okno A");
                    // The comparison offers the stored questions with their answers as one choice;
                    // without the answer key, choosing them would save the quiz with no answers.
                    assertThat(((QuizEntity) conflict.getCurrent()).getAnswers()).isNotEmpty();
                });

        assertThat(quizzes.get("quiz-1").getName()).isEqualTo("Okno A");
        Mockito.verify(auditLog, Mockito.never()).success(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private static final Instant T1 = Instant.parse("2026-11-01T10:00:00.123Z");

    /** A stored quiz, readable through the template the service loads with. */
    private VersionedCollectionFake<QuizEntity> storedQuiz() {
        VersionedCollectionFake<QuizEntity> quizzes =
                new VersionedCollectionFake<>(mongoTemplate, QuizEntity.class, MongoCollections.QUIZ);
        quizzes.put(QuizEntity.builder().id("quiz-1").name("Kvíz").creatorId("alice").updated(T1).build());
        Mockito.when(mongoTemplate.findOne(Mockito.any(), Mockito.eq(QuizEntity.class), Mockito.anyString()))
                .thenAnswer(call -> {
                    Query query = call.getArgument(0);
                    QuizEntity found = quizzes.find(String.valueOf(query.getQueryObject().get("_id"))).orElse(null);
                    // Honour the projection like Mongo does, so a read that leaves the answer key
                    // out really comes back without it.
                    if (found != null && Integer.valueOf(0).equals(query.getFieldsObject().get("answers"))) {
                        return found.toBuilder().answers(null).build();
                    }
                    return found;
                });
        // A plain save overwrites whatever is stored, as the repository would.
        Mockito.when(quizRepository.save(Mockito.any())).thenAnswer(call -> {
            quizzes.put(call.getArgument(0));
            return call.getArgument(0);
        });
        return quizzes;
    }

    private QuizEntity edit(String name, Instant basedOn) {
        return QuizEntity.builder().id("quiz-1").name(name).timeLimit(0).updated(basedOn)
                .answers(List.of(cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData.builder()
                        .questionId("q-1").acceptableAnswers(List.of("správná odpověď")).build()))
                .build();
    }

    private Query capturedQuery() {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        Mockito.verify(mongoTemplate).findOne(captor.capture(), Mockito.eq(QuizEntity.class),
                Mockito.eq(MongoCollections.QUIZ));
        return captor.getValue();
    }
}
