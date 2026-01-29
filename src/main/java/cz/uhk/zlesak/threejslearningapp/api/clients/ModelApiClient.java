package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * ModelApiClient provides connection to the backend service for managing models.
 * It implements the IFileApiClient interface and provides methods for creating, retrieving, uploading, downloading, and deleting model entities.
 * It uses RestClient for making HTTP requests to the backend service.
 * The base URL for the API is determined by the IApiClient interface.
 */
@Component
public class ModelApiClient extends AbstractFileApiClient<ModelEntity, QuickModelEntity, ModelFilter> {

    /**
     * Constructor for ModelApiClient.
     *
     * @param restClient RestClient for making HTTP requests
     * @param objectMapper the ObjectMapper used for JSON serialization/deserialization
     */
    @Autowired
    public ModelApiClient(RestClient restClient, ObjectMapper objectMapper) {
        super(restClient, objectMapper, "model/");
    }

    //region Overridden CRUD operations from IApiClient


    /**
     * Gets a model by ID.
     *
     * @param modelId ID of the model to retrieve
     * @return Model entity
     * @throws Exception if API call fails
     */
    @Override
    public ModelEntity read(String modelId) throws Exception { //TODO BE implementation to provide this structure directly
        InputStreamMultipartFile fileEntity = downloadFileEntity(modelId);
        return ModelEntity.builder()
                .id(modelId)
                .name(fileEntity.getName())
                .fullMainTexture(null)
                .fullOtherTextures(List.of())
                .inputStreamMultipartFile(fileEntity)
                .build();
    }
    //endregion

    //region Overridden operations from AbstractApiClient

    /**
     * Gets the class type of the entity.
     *
     * @return Class of ModelEntity
     */
    @Override
    protected Class<ModelEntity> getEntityClass() {
        return ModelEntity.class;
    }

    /**
     * Gets the class type of the quick entity.
     *
     * @return Class of QuickModelEntity
     */
    @Override
    protected Class<QuickModelEntity> getQuicEntityClass() {
        return QuickModelEntity.class;
    }

    /**
     * Prepares the body for file upload.
     *
     * @param entity the model entity containing the file to upload
     * @return MultiValueMap with the file data
     */
    @Override
    protected MultiValueMap<String, Object> prepareFileUploadBody(ModelEntity entity) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", entity.getInputStreamMultipartFile().getResource());
        entity.setInputStreamMultipartFile(null);
        return body;
    }
    //endregion

    //TODO: Remove after list url is unified in the BE
    @Override
    protected String pageRequestToQueryParams(FilterParameters<ModelFilter> filterParameters, String customBaseUrl) {
        customBaseUrl = customBaseUrl == null ? "list-by" : customBaseUrl;
        return super.pageRequestToQueryParams(filterParameters, customBaseUrl);
    }
}
