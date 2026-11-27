package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ChapterRepository;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.FullTextDocument;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.MongoCollections;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.VersionedWrites;
import cz.uhk.zlesak.threejslearningapp.testsupport.VersionedCollectionFake;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChapterBackendServiceTest {

    private ChapterRepository chapterRepository;
    private ModelBackendService modelBackendService;
    private FullTextSearchService fullTextSearchService;
    private CurrentUserProvider currentUserProvider;
    private QuizBackendService quizBackendService;
    private ChapterBackendService chapterBackendService;
    private MongoTemplate mongoTemplate;
    private AuditLog auditLog;
    private VersionedCollectionFake<ChapterEntity> chapters;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        chapterRepository = Mockito.mock(ChapterRepository.class);
        modelBackendService = Mockito.mock(ModelBackendService.class);
        fullTextSearchService = Mockito.mock(FullTextSearchService.class);
        currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        quizBackendService = Mockito.mock(QuizBackendService.class);

        Mockito.when(currentUserProvider.requireUserId()).thenReturn("alice");
        // The author-or-teacher rule itself is what these tests are about, so it runs for real on
        // top of the stubbed id and role above.
        Mockito.doCallRealMethod().when(currentUserProvider)
                .requireAuthorOrTeacher(Mockito.any(), Mockito.anyString());
        mongoTemplate = Mockito.mock(MongoTemplate.class);
        auditLog = Mockito.mock(AuditLog.class);
        chapters = new VersionedCollectionFake<>(mongoTemplate, ChapterEntity.class, MongoCollections.CHAPTER);
        Mockito.when(chapterRepository.findById(Mockito.anyString()))
                .thenAnswer(call -> chapters.find(call.getArgument(0)));
        Mockito.when(chapterRepository.save(Mockito.any())).thenAnswer(call -> {
            chapters.put(call.getArgument(0));
            return call.getArgument(0);
        });
        Mockito.when(modelBackendService.findAll(Mockito.any())).thenReturn(List.of());
        Mockito.when(quizBackendService.byChapter(Mockito.any())).thenReturn(List.of());

        ObjectProvider<QuizBackendService> quizLookup = Mockito.mock(ObjectProvider.class);
        Mockito.when(quizLookup.getObject()).thenReturn(quizBackendService);

        chapterBackendService = new ChapterBackendService(chapterRepository, modelBackendService,
                fullTextSearchService, Mockito.mock(ListingQueries.class), currentUserProvider,
                mongoTemplate, auditLog, new VersionedWrites(mongoTemplate), quizLookup);
    }

    @Test
    void indexesAChapterForSearchWhenItIsStored() {
        ChapterEntity chapter = chapter("Mozek", "{\"blocks\":[]}", List.of("model-1"));

        ChapterEntity saved = chapterBackendService.create(chapter);

        assertThat(saved.getCreatorId()).isEqualTo("alice");
        assertThat(saved.getCreated()).isNotNull();
        Mockito.verify(fullTextSearchService).index(Mockito.any(), Mockito.eq(FullTextDocument.FullTextType.CHAPTER),
                Mockito.eq("Mozek"), Mockito.eq("{\"blocks\":[]}"));
    }

    @Test
    void rejectsAChapterThatIsMissingWhatItNeedsToRender() {
        assertThatThrownBy(() -> chapterBackendService.create(chapter(" ", "obsah", List.of("m"))))
                .isInstanceOf(BackendException.Validation.class).hasMessageContaining("Název");
        assertThatThrownBy(() -> chapterBackendService.create(chapter("Mozek", " ", List.of("m"))))
                .isInstanceOf(BackendException.Validation.class).hasMessageContaining("Obsah");
        assertThatThrownBy(() -> chapterBackendService.create(chapter("Mozek", "obsah", List.of())))
                .isInstanceOf(BackendException.Validation.class).hasMessageContaining("alespoň jeden model");
    }

    @Test
    void refusesAChapterPointingAtAModelThatDoesNotExist() {
        Mockito.doThrow(new BackendException.NotFound("Model s ID gone nebyl nalezen."))
                .when(modelBackendService).require("gone");

        assertThatThrownBy(() -> chapterBackendService.create(chapter("Mozek", "obsah", List.of("gone"))))
                .isInstanceOf(BackendException.NotFound.class);
    }

    @Test
    void keepsTheOriginalAuthorAndCreationTimeOnUpdate() {
        ChapterEntity existing = chapter("Mozek", "obsah", List.of("model-1"));
        existing.setId("ch-1");
        existing.setCreatorId("alice");
        existing.setCreated(Instant.parse("2020-01-01T00:00:00Z"));
        chapters.put(existing);

        ChapterEntity incoming = chapter("Mozek upraveno", "nový obsah", List.of("model-1"));
        incoming.setId("ch-1");
        incoming.setCreatorId("someone-else");

        ChapterEntity saved = chapterBackendService.update(incoming);

        assertThat(saved.getCreatorId()).isEqualTo("alice");
        assertThat(saved.getCreated()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(saved.getUpdated()).isNotNull();
    }

    @Test
    void letsOnlyTheAuthorOrATeacherEditAChapter() {
        ChapterEntity existing = chapter("Mozek", "obsah", List.of("model-1"));
        existing.setId("ch-1");
        existing.setCreatorId("someone-else");
        chapters.put(existing);
        Mockito.when(currentUserProvider.hasRole("TEACHER")).thenReturn(false);

        ChapterEntity incoming = chapter("Mozek", "obsah", List.of("model-1"));
        incoming.setId("ch-1");

        assertThatThrownBy(() -> chapterBackendService.update(incoming))
                .isInstanceOf(BackendException.Forbidden.class);

        Mockito.when(currentUserProvider.hasRole("TEACHER")).thenReturn(true);
        assertThat(chapterBackendService.update(incoming).getCreatorId()).isEqualTo("someone-else");
    }

    @Test
    void dropsTheSearchEntryAndDetachesTheQuizzesWhenAChapterIsDeleted() {
        ChapterEntity stored = chapter("Mozek", "obsah", List.of("model-1"));
        stored.setId("ch-1");
        Mockito.when(chapterRepository.findById("ch-1")).thenReturn(Optional.of(stored));

        chapterBackendService.delete("ch-1");

        Mockito.verify(chapterRepository).deleteById("ch-1");
        Mockito.verify(fullTextSearchService).remove("ch-1");
        // Left attached, a quiz would resolve a chapter that no longer exists and could then be
        // neither opened for editing nor saved.
        Mockito.verify(quizBackendService).detachFromChapter("ch-1");
    }

    @Test
    void letsOnlyTheAuthorOrATeacherDeleteAChapter() {
        ChapterEntity stored = chapter("Mozek", "obsah", List.of("model-1"));
        stored.setId("ch-1");
        stored.setCreatorId("someone-else");
        Mockito.when(chapterRepository.findById("ch-1")).thenReturn(Optional.of(stored));
        Mockito.when(currentUserProvider.hasRole("TEACHER")).thenReturn(false);

        assertThatThrownBy(() -> chapterBackendService.delete("ch-1"))
                .isInstanceOf(BackendException.Forbidden.class);

        Mockito.verify(chapterRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verify(quizBackendService, Mockito.never()).detachFromChapter(Mockito.any());

        Mockito.when(currentUserProvider.hasRole("TEACHER")).thenReturn(true);
        chapterBackendService.delete("ch-1");
        Mockito.verify(chapterRepository).deleteById("ch-1");
    }

    @Test
    void refusesToDeleteAChapterThatDoesNotExist() {
        Mockito.when(chapterRepository.findById("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterBackendService.delete("gone"))
                .isInstanceOf(BackendException.NotFound.class);

        Mockito.verify(chapterRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verify(quizBackendService, Mockito.never()).detachFromChapter(Mockito.any());
    }

    @Test
    void readingAChapterResolvesItsModelsAndQuizzes() {
        ChapterEntity stored = chapter("Mozek", "obsah", List.of("model-1"));
        stored.setId("ch-1");
        Mockito.when(chapterRepository.findById("ch-1")).thenReturn(Optional.of(stored));
        Mockito.when(modelBackendService.findAll(List.of("model-1")))
                .thenReturn(List.of(QuickModelEntity.builder().id("model-1").name("Lebka").build()));
        Mockito.when(quizBackendService.byChapter("ch-1"))
                .thenReturn(List.of(QuizEntity.builder().id("quiz-1").name("Kvíz").build()));

        ChapterEntity loaded = chapterBackendService.require("ch-1");

        assertThat(loaded.getModels()).extracting(QuickModelEntity::getId).containsExactly("model-1");
        assertThat(loaded.getQuizzes()).singleElement()
                .satisfies(quiz -> assertThat(quiz.getName()).isEqualTo("Kvíz"));
    }

    @Test
    void reportsAMissingChapterRatherThanReturningNothing() {
        Mockito.when(chapterRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterBackendService.require("nope"))
                .isInstanceOf(BackendException.NotFound.class);
        assertThatThrownBy(() -> chapterBackendService.require(""))
                .isInstanceOf(BackendException.Validation.class);
    }

    @Test
    void aSaveBasedOnTheStoredVersionGoesThrough() {
        chapters.put(storedChapter("Mozek", T1));

        ChapterEntity saved = chapterBackendService.update(edit("Mozek upraveno", T1));

        assertThat(chapters.get("ch-1").getName()).isEqualTo("Mozek upraveno");
        // The new version is what the next save from this editor must quote, so it has to be the
        // value Mongo keeps: a finer one would make that save conflict with itself.
        assertThat(saved.getUpdated()).isAfter(T1).isEqualTo(saved.getUpdated().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(chapters.get("ch-1").getUpdated()).isEqualTo(saved.getUpdated());
    }

    @Test
    void aStaleSaveIsRejectedAndTheOtherEditStays() {
        chapters.put(storedChapter("Mozek", T1));
        chapterBackendService.update(edit("Okno A", T1));

        assertThatThrownBy(() -> chapterBackendService.update(edit("Okno B", T1)))
                .isInstanceOfSatisfying(BackendException.Conflict.class, conflict ->
                        assertThat(conflict.getCurrent().getName()).isEqualTo("Okno A"));

        assertThat(chapters.get("ch-1").getName()).isEqualTo("Okno A");
    }

    @Test
    void aRejectedSaveReportsNoSuccess() {
        chapters.put(storedChapter("Mozek", T1));
        chapterBackendService.update(edit("Okno A", T1));
        Mockito.clearInvocations(fullTextSearchService, auditLog);

        assertThatThrownBy(() -> chapterBackendService.update(edit("Okno B", T1)))
                .isInstanceOf(BackendException.Conflict.class);

        Mockito.verify(auditLog, Mockito.never()).success(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(fullTextSearchService, Mockito.never()).index(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void checkAndWriteAreOneConditionalOperation() {
        chapters.put(storedChapter("Mozek", T1));

        chapterBackendService.update(edit("Mozek upraveno", T1));

        // Loading, comparing and then saving would leave a window in which a second save passes the
        // same check; only a write whose filter carries the version closes it.
        Mockito.verify(chapterRepository, Mockito.never()).save(Mockito.any());
        org.mockito.ArgumentCaptor<org.springframework.data.mongodb.core.query.Query> query =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.mongodb.core.query.Query.class);
        Mockito.verify(mongoTemplate).findAndReplace(query.capture(), Mockito.any(),
                Mockito.any(), Mockito.eq(ChapterEntity.class), Mockito.eq(MongoCollections.CHAPTER));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", "ch-1")
                .containsEntry("updated", java.util.Date.from(T1));
    }

    @Test
    void aChapterWrittenBeforeTimestampsCanStillBeSaved() {
        chapters.put(storedChapter("Mozek", null));

        chapterBackendService.update(edit("Mozek upraveno", null));

        assertThat(chapters.get("ch-1").getName()).isEqualTo("Mozek upraveno");
    }

    private static final Instant T1 = Instant.parse("2026-11-01T10:00:00.123Z");

    private ChapterEntity storedChapter(String name, Instant updated) {
        ChapterEntity stored = chapter(name, "obsah", List.of("model-1"));
        stored.setId("ch-1");
        stored.setCreatorId("alice");
        stored.setUpdated(updated);
        return stored;
    }

    private ChapterEntity edit(String name, Instant basedOn) {
        ChapterEntity edit = chapter(name, "obsah " + name, List.of("model-1"));
        edit.setId("ch-1");
        edit.setUpdated(basedOn);
        return edit;
    }

    private ChapterEntity chapter(String name, String content, List<String> modelIds) {
        return ChapterEntity.builder().name(name).content(content).modelIds(modelIds).build();
    }
}
