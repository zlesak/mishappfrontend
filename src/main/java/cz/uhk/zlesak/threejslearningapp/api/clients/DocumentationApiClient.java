package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * DocumentationApiClient handles API requests related to DocumentationEntry entities.
 * Documentation is currently managed locally (file-based), so BE integration is not active.
 */
@Component
public class DocumentationApiClient extends AbstractApiClient<DocumentationEntry, DocumentationEntry, DocumentationFilter> {

    /**
     * Constructor for DocumentationApiClient.
     *
     * @param restClient   RestClient for making HTTP requests.
     * @param objectMapper ObjectMapper for JSON serialization/deserialization.
     */
    @Autowired
    public DocumentationApiClient(RestClient restClient, ObjectMapper objectMapper) {
        super(restClient, objectMapper, "documentation/");
    }

    @Override
    protected Class<DocumentationEntry> getEntityClass() {
        return DocumentationEntry.class;
    }

    @Override
    protected Class<DocumentationEntry> getQuicEntityClass() {
        return DocumentationEntry.class;
    }
}
