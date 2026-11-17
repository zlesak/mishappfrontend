package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ChapterRepository;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.FullTextDocument;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.MongoCollections;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.QuickChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Owns learning chapters: their content, the models they refer to and their search index entry.
 * A chapter stores only the ids of its models; the models themselves are read from their own
 * collection, so a chapter never carries a stale copy of one.
 */
@Service
@RequiredArgsConstructor
public class ChapterBackendService {

    private final ChapterRepository chapterRepository;
    private final ModelBackendService modelBackendService;
    private final FullTextSearchService fullTextSearchService;
    private final ListingQueries listingQueries;
    private final CurrentUserProvider currentUserProvider;
    private final MongoTemplate mongoTemplate;
    private final AuditLog auditLog;
    /** Quizzes reference chapters and chapters list their quizzes; the lazy lookup breaks the cycle. */
    private final ObjectProvider<QuizBackendService> quizLookup;

    /**
     * Stores a new chapter and indexes it for search.
     *
     * @param chapter chapter to store.
     * @return the stored chapter, with its assigned id and models resolved.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public ChapterEntity create(ChapterEntity chapter) {
        validate(chapter);
        chapter.setId(null);
        // Audit is written after the save, once there is an id to record.
        chapter.setCreatorId(currentUserProvider.requireUserId());
        chapter.setCreatorName(currentUserProvider.currentUserName());
        Instant now = Instant.now();
        chapter.setCreated(chapter.getCreated() != null ? chapter.getCreated() : now);
        chapter.setUpdated(now);

        ChapterEntity saved = afterWrite(chapterRepository.save(chapter));
        auditLog.success(AuditLog.Action.CREATE, "chapter", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * Updates an existing chapter. The original author is preserved: a chapter cannot change owners.
     *
     * @param chapter chapter carrying the new content; its id must be set.
     * @return the stored chapter, with its models resolved.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public ChapterEntity update(ChapterEntity chapter) {
        if (chapter.getId() == null || chapter.getId().isBlank()) {
            throw new BackendException.Validation("Kapitolu nelze aktualizovat bez ID.");
        }
        ChapterEntity existing = load(chapter.getId());
        validate(chapter);

        String editorId = currentUserProvider.requireUserId();
        currentUserProvider.requireAuthorOrTeacher(existing.getCreatorId(),
                "Kapitolu může upravit pouze její autor.");

        chapter.setCreatorId(existing.getCreatorId() != null ? existing.getCreatorId() : editorId);
        chapter.setCreatorName(existing.getCreatorName() != null
                ? existing.getCreatorName()
                : currentUserProvider.currentUserName());
        chapter.setCreated(existing.getCreated());
        chapter.setUpdated(Instant.now());

        ChapterEntity saved = afterWrite(chapterRepository.save(chapter));
        auditLog.success(AuditLog.Action.UPDATE, "chapter", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * @param chapterId id of the chapter.
     * @return the chapter with its models resolved.
     * @throws BackendException.NotFound when no such chapter exists.
     */
    public ChapterEntity require(String chapterId) {
        ChapterEntity chapter = resolveModels(load(chapterId));
        chapter.setQuizzes(quizzesOf(chapterId));
        return chapter;
    }

    /**
     * Removes a chapter, its search index entry and the chapter reference of its quizzes.
     * Detaching the quizzes is what keeps them usable: a quiz still pointing at a deleted chapter
     * could neither be opened for editing nor saved again.
     *
     * @param chapterId id of the chapter.
     * @throws BackendException.NotFound when no such chapter exists.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public void delete(String chapterId) {
        ChapterEntity chapter = load(chapterId);
        currentUserProvider.requireAuthorOrTeacher(chapter.getCreatorId(),
                "Kapitolu může smazat pouze její autor.");
        quizLookup.getObject().detachFromChapter(chapter.getId());
        chapterRepository.deleteById(chapter.getId());
        fullTextSearchService.remove(chapter.getId());
        auditLog.success(AuditLog.Action.DELETE, "chapter", chapter.getId(), chapter.getName());
    }

    /**
     * @param filterParameters paging and filtering requested by the UI.
     * @return one page of chapters, without their content.
     */
    public PageResult<QuickChapterEntity> list(FilterParameters<ChapterFilter> filterParameters) {
        ChapterFilter filter = filterParameters.getFilter();
        String term = listingQueries.searchTerm(filter);

        Query query;
        if (term == null) {
            query = listingQueries.baseQuery(filter);
        } else {
            // A chapter's text is what users remember, so the search looks inside the content as
            // well as at the name. The name criterion is added here rather than by baseQuery so the
            // two can be OR-ed, while every other filter — author, date range — still has to hold.
            query = listingQueries.baseQueryWithoutName(filter);
            Criteria byName = Criteria.where("name").regex(java.util.regex.Pattern.quote(term), "i");
            List<String> contentMatches = fullTextSearchService.search(term, FullTextDocument.FullTextType.CHAPTER);
            query.addCriteria(contentMatches.isEmpty()
                    ? byName
                    : new Criteria().orOperator(byName, Criteria.where("_id").in(contentMatches)));
        }

        // Filtering by a model the chapter contains. The chapter document holds only model ids, so the
        // name is resolved to ids first; a name that matches nothing must yield no chapters rather than
        // being quietly ignored.
        if (filter != null && filter.getModelName() != null && !filter.getModelName().isBlank()) {
            List<String> modelIds = modelBackendService.idsByName(filter.getModelName());
            query.addCriteria(Criteria.where("modelIds").in(modelIds));
        }

        query.fields().exclude("content");
        PageResult<QuickChapterEntity> page = listingQueries.page(query, filterParameters.getPageRequest(),
                QuickChapterEntity.class, MongoCollections.CHAPTER);
        resolveModels(page.elements());
        return page;
    }

    /**
     * @param keyword text typed by the user.
     * @return chapters whose name or content matches.
     */
    public List<QuickChapterEntity> searchFullText(String keyword) {
        List<String> ids = fullTextSearchService.search(keyword, FullTextDocument.FullTextType.CHAPTER);
        if (ids.isEmpty()) {
            return List.of();
        }

        Query query = new Query(Criteria.where("_id").in(ids));
        query.fields().exclude("content");
        return mongoTemplate.find(query, QuickChapterEntity.class, MongoCollections.CHAPTER);
    }

    /**
     * @param modelId id of a model.
     * @return chapters that refer to the model, so the UI can explain why it cannot be deleted.
     */
    public List<ChapterEntity> usingModel(String modelId) {
        return chapterRepository.findByModelIdsContaining(modelId);
    }

    private List<QuickQuizEntity> quizzesOf(String chapterId) {
        return quizLookup.getObject().byChapter(chapterId).stream()
                .map(QuickQuizEntity.class::cast)
                .toList();
    }

    private ChapterEntity load(String chapterId) {
        if (chapterId == null || chapterId.isBlank()) {
            throw new BackendException.Validation("ID kapitoly nesmí být prázdné.");
        }
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BackendException.NotFound("Kapitola s ID " + chapterId + " nebyla nalezena."));
    }

    private ChapterEntity afterWrite(ChapterEntity saved) {
        fullTextSearchService.index(saved.getId(), FullTextDocument.FullTextType.CHAPTER,
                saved.getName(), saved.getContent());
        return resolveModels(saved);
    }

    private ChapterEntity resolveModels(ChapterEntity chapter) {
        resolveModels(List.of(chapter));
        return chapter;
    }

    /**
     * Fills in the models of several chapters with a single read, so listing a page of chapters
     * does not turn into one query per chapter.
     */
    private void resolveModels(List<? extends QuickChapterEntity> chapters) {
        List<String> modelIds = chapters.stream()
                .map(QuickChapterEntity::getModelIds)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .toList();

        Map<String, QuickModelEntity> byId = modelBackendService.findAll(modelIds).stream()
                .collect(Collectors.toMap(QuickModelEntity::getId, model -> model, (first, second) -> first));

        for (QuickChapterEntity chapter : chapters) {
            List<String> ids = chapter.getModelIds() == null ? List.of() : chapter.getModelIds();
            chapter.setModels(ids.stream().map(byId::get).filter(Objects::nonNull).toList());
        }
    }

    /**
     * Rejects chapters that are incomplete or refer to models that no longer exist, which would
     * otherwise render as a broken chapter.
     */
    private void validate(ChapterEntity chapter) {
        if (chapter.getName() == null || chapter.getName().isBlank()) {
            throw new BackendException.Validation("Název kapitoly nesmí být prázdný.");
        }
        if (chapter.getContent() == null || chapter.getContent().isBlank()) {
            throw new BackendException.Validation("Obsah kapitoly nesmí být prázdný.");
        }
        if (chapter.getModelIds() == null || chapter.getModelIds().isEmpty()) {
            throw new BackendException.Validation("Kapitola musí mít alespoň jeden model.");
        }

        for (String modelId : chapter.getModelIds()) {
            modelBackendService.require(modelId);
        }
    }
}
