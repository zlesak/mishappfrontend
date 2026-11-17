package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.QuickChapterEntity;

import java.util.List;

/**
 * Chapter data access.
 */
public interface IChapterApiClient extends IApiClient<ChapterEntity, QuickChapterEntity, ChapterFilter> {

    /**
     * Finds chapters by a free-text query over their name and content.
     *
     * @param keyword text typed by the user.
     * @return matching chapters in listing form.
     * @throws Exception if the search fails.
     */
    List<QuickChapterEntity> search(String keyword) throws Exception;
}
