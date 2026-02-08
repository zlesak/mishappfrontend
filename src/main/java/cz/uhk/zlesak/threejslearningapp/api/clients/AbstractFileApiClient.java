package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractFileEntity;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Abstract file api client provides common functionality for file-related API clients.
 *
 * @param <E> entity type
 * @param <Q> quick entity type
 * @param <F> filter type
 */
public abstract class AbstractFileApiClient<E extends Q, Q extends AbstractFileEntity, F> extends AbstractApiClient<E, Q, F> {

    private final String type;

    /**
     * Constructor for AbstractFileApiClient.
     *
     * @param restClient rest client
     * @param objectMapper object mapper
     * @param endpoint     API endpoint
     */
    public AbstractFileApiClient(RestClient restClient, ObjectMapper objectMapper, String endpoint) {
        super(restClient, objectMapper, endpoint);
        this.type = endpoint.equals("model/") ? "model" : endpoint.equals("texture/") ? "texture" : "file";
    }

    //region Overridden operations from IApiClient

    /**
     * Overridden update method to throw not implemented exception.
     *
     * @param textureId     ID of the texture to update
     * @param textureEntity texture entity
     * @return updated texture entity
     * @throws Exception throws exception when update fails
     */
    @Override
    public E update(String textureId, E textureEntity) throws Exception {
        throw new NotImplementedException("Aktualizace textur není zatím implementováno.");
    }
    //endregion

    //region Methods from IFileApiClient


    protected abstract MultiValueMap<String, Object> prepareFileUploadBody(E entity);

    @Override
    public Q create(E entity) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String token = getJwtToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }

        MultiValueMap<String, Object> body = prepareFileUploadBody(entity);

        String metadataJson = objectMapper.writeValueAsString(entity);
        HttpHeaders metadataHeaders = new HttpHeaders();
        metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> metadataPart = new HttpEntity<>(metadataJson, metadataHeaders);
        body.add("metadata", metadataPart);

        return sendPostRequest(baseUrl + "upload", body, getQuicEntityClass(), "Chyba při nahrávání " + type + " " + getEntityClass().getSimpleName(), null, headers);
    }


    /**
     * @param fileId ID of the file to download
     * @return downloaded file
     * @throws Exception throws exception when download fails
     */

    public InputStreamMultipartFile downloadFileEntity(String fileId) throws Exception {
        String url = baseUrl + "download/" + fileId;
        ResponseEntity<byte[]> response = sendGetRequestRaw(url, byte[].class, "Chyba při stahování " + type + " dle ID", fileId, true);
        return parseFileResponse(response, "Nenalezeno nebo chyba při stahování." + getEntityClass().getSimpleName(), fileId);
    }
    //endregion

    public static String getStreamBeEndpointUrl(String id, String type) {
        return IApiClient.getExternalAppUrl() + type + "/download/" + id;
    }
}
