package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.ModelApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Service for managing 3D models, including uploading and retrieving model files and textures.
 * Model uploads (including textures and CSV files) are handled in a single API call via ModelApiClient.
 *
 * @see ModelApiClient
 */
@Service
@Scope("prototype")
public class ModelService extends AbstractService<ModelEntity, QuickModelEntity, ModelFilter> {

    /**
     * Constructor for ModelService.
     *
     * @param modelApiClient the API client for interacting with model-related endpoints.
     */
    @Autowired
    public ModelService(ModelApiClient modelApiClient) {
        super(modelApiClient);
    }

    /**
     * Validates the create model entity.
     *
     * @param createModelEntity the model entity to validate
     * @throws RuntimeException if validation fails
     */
    @Override
    protected ModelEntity validateCreateEntity(ModelEntity createModelEntity) throws RuntimeException {
        if (createModelEntity.getName().isEmpty()) {
            throw new ApplicationContextException("Název modelu nesmí být prázdný.");
        }
        if (createModelEntity.getInputStreamMultipartFile() == null) {
            throw new ApplicationContextException("Soubor pro nahrání modelu nesmí být prázdný.");
        }
        return createModelEntity;
    }

    /**
     * Creates the final model entity from the create model entity.
     * Returns the entity unchanged – all file data is needed by {@link ModelApiClient#create(ModelEntity)}.
     */
    @Override
    protected ModelEntity createFinalEntity(ModelEntity createModelEntity) throws RuntimeException {
        return createModelEntity;
    }

    /**
     * Downloads a single file from the backend by its ID.
     *
     * @param fileId ID of the file to download
     * @return the file as InputStreamMultipartFile
     */
    public InputStreamMultipartFile downloadFile(String fileId) throws Exception {
        return ((ModelApiClient) apiClient).downloadFile(fileId);
    }
}

