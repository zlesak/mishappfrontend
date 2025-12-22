package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.contracts.ITextureApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureFilter;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * TextureApiClient provides connection to the backend service for managing textures.
 * It implements the ITextureApiClient interface and provides methods for creating, retrieving, uploading, downloading, and deleting texture entities.
 * It uses RestTemplate for making HTTP requests to the backend service.
 * The base URL for the API is determined by the IApiClient interface.
 */
@Component
public class TextureApiClient extends AbstractFileApiClient<TextureEntity, QuickTextureEntity, TextureFilter> implements ITextureApiClient {
    /**
     * Constructor for TextureApiClient.
     *
     * @param restTemplate the RestTemplate used for making HTTP requests
     * @param objectMapper the ObjectMapper used for JSON serialization/deserialization
     */
    @Autowired
    public TextureApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper, "texture/");
    }

    //region Overridden CRUD operations from IApiClient

    /**
     * Gets a texture by ID.
     *
     * @param textureId ID of the texture to retrieve
     * @return Texture entity
     * @throws Exception if API call fails
     */
    @Override
    public TextureEntity read(String textureId) throws Exception {//TODO BE implementation to provide this structure directly
        InputStreamMultipartFile file = downloadFileEntity(textureId);
        return TextureEntity.builder()
                .id(textureId)
                .name(file.getName())
                .textureFile(file)
                .build();
    }

    /**
     * Gets paginated list of textures with filtering.
     *
     * @param filterParameters FilterParameters<TextureFilter> object containing pagination info and filters
     * @return PageResult of QuickTextureEntity
     * @throws Exception if API call fails
     */
    @Override
    public PageResult<QuickTextureEntity> readEntities(FilterParameters<TextureFilter> filterParameters) throws Exception {
        throw new NotImplementedException("Filtrování textur není zatím implementováno.");
    }

    //endregion

    //region Overridden operations from AbstractApiClient

    /**
     * Gets the class type of the entity.
     *
     * @return Class of TextureEntity
     */
    @Override
    protected Class<TextureEntity> getEntityClass() {
        return TextureEntity.class;
    }

    /**
     * Gets the class type of the quick entity.
     *
     * @return Class of QuickTextureEntity
     */
    @Override
    protected Class<QuickTextureEntity> getQuicEntityClass() {
        return QuickTextureEntity.class;
    }

    /**
     * Prepares the body for file upload.
     *
     * @param entity the model entity containing the file to upload
     * @return MultiValueMap with the file data
     */
    @Override
    protected MultiValueMap<String, Object> prepareFileUploadBody(TextureEntity entity) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("texture", entity.getTextureFile().getResource());
        entity.setTextureFile(null);
        return body;
    }

    //endregion
}
