package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IChapterApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * ChapterApiClient is responsible for handling API requests related to Chapter entities.
 * It extends AbstractApiClient to inherit common API client functionality.
 */
@Component
public class ChapterApiClient extends AbstractApiClient<ChapterEntity, ChapterEntity, ChapterFilter> implements IChapterApiClient { //TODO QUICKCHAPTERENTITY
    /**
     * Constructor for ChapterApiClient.
     *
     * @param restTemplate RestTemplate for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     */
    @Autowired
    public ChapterApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper, "chapter/");
    }

    //region Overridden operations from AbstractApiClient
    /**
     * Gets the entity class for Chapter
     * @return ChapterEntity class
     */
    @Override
    protected Class<ChapterEntity> getEntityClass() {
        return ChapterEntity.class;
    }

    /**
     * Gets the quick entity class for Chapter
     * @return ChapterEntity class
     */
    @Override
    protected Class<ChapterEntity> getQuicEntityClass() { //TODO after QuickChapterEntity created and existing on BE
        return ChapterEntity.class;
    }
    //endregion
}