package cz.uhk.zlesak.threejslearningapp.api;

import cz.uhk.zlesak.threejslearningapp.api.contracts.IChapterApiClient;
import cz.uhk.zlesak.threejslearningapp.backend.service.ChapterBackendService;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.QuickChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chapter access for the UI.
 */
@Component
@RequiredArgsConstructor
public class ChapterApiClient implements IChapterApiClient {

    private final ChapterBackendService chapterBackendService;

    @Override
    public QuickChapterEntity create(ChapterEntity entity) {
        return chapterBackendService.create(entity);
    }

    @Override
    public ChapterEntity read(String id) {
        return chapterBackendService.require(id);
    }

    @Override
    public QuickChapterEntity readQuick(String id) {
        return chapterBackendService.require(id);
    }

    @Override
    public PageResult<QuickChapterEntity> readEntities(FilterParameters<ChapterFilter> filterParameters) {
        return chapterBackendService.list(filterParameters);
    }

    @Override
    public ChapterEntity update(String id, ChapterEntity entity) {
        entity.setId(id);
        return chapterBackendService.update(entity);
    }

    @Override
    public boolean delete(String id) {
        chapterBackendService.delete(id);
        return true;
    }

    @Override
    public List<QuickChapterEntity> search(String keyword) {
        return chapterBackendService.searchFullText(keyword);
    }
}
