package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.ModelApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing 3D models, including uploading and retrieving model files and textures.
 * This class handles the interaction with the model API client to upload models and textures,
 * and provides methods to retrieve model files, names, and base64 representations.
 * It also integrates with the textureService to manage textures associated with the models as the textures are an integral part of the model data.
 *
 * @see TextureService
 */
@Slf4j
@Service
@Scope("prototype")
public class ModelService extends AbstractService<ModelEntity, QuickModelEntity, ModelFilter> {
    private final TextureService textureService;
    private final ModelApiClient modelApiClient;

    /**
     * Constructor for ModelService.
     * Initializes controller with dependencies for texture management, model API client, and JSON processing.
     *
     * @param textureService the controller for managing textures associated with models.
     * @param modelApiClient the API client for interacting with model-related endpoints.
     */
    @Autowired
    public ModelService(TextureService textureService, ModelApiClient modelApiClient) {
        super(modelApiClient);
        this.textureService = textureService;
        this.modelApiClient = modelApiClient;
    }

    /**
     * Retrieves the name of a model by its ID.
     * If the model entity is not already loaded or if the loaded entity does not match the requested ID,
     * it fetches the model entity using the getModel method.
     *
     * @param modelId the ID of the model whose name is to be retrieved.
     * @return the name of the model.
     */
    public String getModelName(String modelId) {
        read(modelId);
        return entity.getName();
    }

    /**
     * Retrieves the InputStream of a model file by its ID.
     * If the model entity is not already loaded or if the loaded entity does not match the requested ID,
     * it fetches the model entity using the getModel method.
     *
     * @param modelId the ID of the model whose InputStream is to be retrieved.
     * @return the InputStream of the model file.
     */
    public InputStreamResource getInputStream(String modelId) {
        read(modelId);
        return new InputStreamResource(entity.getInputStreamMultipartFile().getInputStream());
    }

    @Override
    public QuickModelEntity create(ModelEntity createEntity) throws RuntimeException {
        QuickModelEntity quickModelEntity;

        try {
            quickModelEntity = apiClient.create(createFinalEntity(validateCreateEntity(createEntity)));

        } catch (Exception e) {
            log.error("Chyba při vytváření enity: {}", e.getMessage(), e);
            throw new RuntimeException("Chyba při vytváření entity: " + e.getMessage(), e);
        }
        if (createEntity.isAdvanced()) {
            try {
                QuickTextureEntity mainTextureQuickFileEntity = textureService.create(
                        TextureEntity.builder()
                                .textureFile(createEntity.getFullMainTexture().getTextureFile())
                                .csvContent(null)
                                .isPrimary(true)
                                .modelId(quickModelEntity.getModel().getId()).build());
                quickModelEntity.setMainTexture(mainTextureQuickFileEntity);
            } catch (Exception e) {
                log.error("Chyba při nahrávání hlavní textury: {}", e.getMessage(), e);
                throw new RuntimeException("Chyba při nahrávání hlavní textury: " + e.getMessage(), e);
            }

            try {
                List<QuickTextureEntity> otherTexturesUploadedList = textureService.createOtherTextures(
                        createOtherTextureEntities(createEntity.getFullOtherTextures(), createEntity.getCsvFiles(), quickModelEntity.getModel().getId())
                );
                quickModelEntity.setOtherTextures(otherTexturesUploadedList);
            } catch (Exception e) {
                log.error("Chyba při nahrávání vedlejších textur: {}", e.getMessage(), e);
                throw new RuntimeException("Chyba při nahrávání vedlejších textur: " + e.getMessage(), e);
            }
        }
        return quickModelEntity;
    }

    /**
     * Retrieves the URL for texture file BE endpoint by texture ID.
     *
     * @param textureId the ID of the texture whose URL is to be retrieved.
     * @return the URL for the texture file BE endpoint.
     */
    public String getModelFileBeEndpointUrl(String textureId) {
        return modelApiClient.getStreamBeEndpointUrl(textureId);
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
        if (createModelEntity.isAdvanced()) {
            if (createModelEntity.getFullMainTexture() == null) {
                throw new ApplicationContextException("Hlavní textura nesmí být prázdná.");
            }
        }
        return createModelEntity;
    }

    /**
     * Creates the final model entity from the create model entity.
     * @param createModelEntity the model entity to create
     * @return the final model entity
     * @throws RuntimeException if creation fails
     */
    @Override
    protected ModelEntity createFinalEntity(ModelEntity createModelEntity) throws RuntimeException {
        return ModelEntity.builder()
                .name(createModelEntity.getName())
                .inputStreamMultipartFile(createModelEntity.getInputStreamMultipartFile())
                .otherTextures(List.of())
                .created(Instant.now())
                .build();
    }

    /**
     * Gets the CSV content for a given texture entity from a list of CSV files.
     * @param textureEntity the texture entity for which to get the CSV content
     * @param csvFiles list of CSV files to search
     * @return the CSV content as a String, or an empty string if not found
     * @throws IOException if an I/O error occurs
     */
    private String getCsvContentForTexture(TextureEntity textureEntity, List<InputStreamMultipartFile> csvFiles) throws IOException {

        InputStreamMultipartFile csv = null;
        InputStream csvStream = null;
        String prefix;
        prefix = textureEntity.getTextureFile().getName().substring(0, textureEntity.getTextureFile().getName().lastIndexOf('.'));
        for (InputStreamMultipartFile csvFile : csvFiles) {
            if (csvFile.getName().equals(prefix + ".csv")) {
                csv = csvFile;
                csvStream = csv.getInputStream();
                break;
            }
        }
        csvFiles.remove(csv);

        return csv == null ? "" : new String(csvStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Creates a list of other texture entities for a given model ID.
     * @param texturesEntities list of texture entities to process
     * @param csvFiles list of CSV files to associate with textures
     * @param modelId the model ID to associate with the textures
     * @return list of created texture entities
     * @throws RuntimeException if an error occurs during creation
     */
    private List<TextureEntity> createOtherTextureEntities(List<TextureEntity> texturesEntities, List<InputStreamMultipartFile> csvFiles, String modelId) throws RuntimeException {
        return texturesEntities.stream()
                .map(textureEntity -> {
                    try {
                        return TextureEntity.builder()
                                .name(textureEntity.getName())
                                .created(Instant.now())
                                .csvContent(getCsvContentForTexture(textureEntity, csvFiles))
                                .textureFile(textureEntity.getTextureFile())
                                .isPrimary(false)
                                .modelId(modelId)
                                .build();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
