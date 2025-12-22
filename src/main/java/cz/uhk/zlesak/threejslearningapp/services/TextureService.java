package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.clients.TextureApiClient;
import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureFilter;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureUpload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * TextureService is responsible for handling texture-related operations such as uploading textures,
 * retrieving texture images, and managing texture data.
 * It interacts with the TextureApiClient to perform these operations.
 */
@Slf4j
@Service
@Scope("prototype")
public class TextureService extends AbstractService<TextureEntity, QuickTextureEntity, TextureFilter> {
    protected final ObjectMapper objectMapper;

    /**
     * Constructor for TextureService.
     * Initializes the controller with the provided TextureApiClient and ObjectMapper.
     *
     * @param textureApiClient client for interacting with the texture API
     * @param objectMapper     object mapper for JSON serialization and deserialization
     */
    @Autowired
    public TextureService(TextureApiClient textureApiClient, ObjectMapper objectMapper) {
        super(textureApiClient);
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new texture entity.
     *
     * @param createEntity Entity to create
     * @return Created texture entity
     * @throws RuntimeException if an error occurs during the creation process
     */
    @Override
    public QuickTextureEntity create(TextureEntity createEntity) throws RuntimeException {
        try {
            return apiClient.create(createFinalEntity(validateCreateEntity(createEntity)));
        } catch (Exception e) {
            throw new RuntimeException("Chyba při nahrávání textury: " + e.getMessage(), e);
        }
    }

    /**
     * Creates multiple other texture entities.
     *
     * @param otherTexturesEntities List of texture entities to create
     * @return List of created texture entities
     * @throws RuntimeException if an error occurs during the creation process
     */
    public List<QuickTextureEntity> createOtherTextures(List<TextureEntity> otherTexturesEntities) throws RuntimeException {
        List<QuickTextureEntity> otherTextureEntities = new ArrayList<>();
        for (TextureEntity otherTextureEntity : otherTexturesEntities) {
            QuickTextureEntity quickTextureEntity = create(otherTextureEntity);
            otherTextureEntities.add(quickTextureEntity);
        }
        return otherTextureEntities;
    }

    /**
     * Retrieves the name of the texture by its ID.
     * If the texture entity is not already cached or if the cached entity does not match the requested ID,
     * it fetches the texture entity from the server.
     *
     * @param textureId the ID of the texture whose name is to be retrieved
     * @return the name of the texture
     */
    public String getTextureName(String textureId) {
        read(textureId);
        return entity.getName();
    }

    /**
     * Retrieves the texture file as an InputStream by its ID.
     * If the texture entity is not already cached or if the cached entity does not match the requested ID,
     * it fetches the texture entity from the server.
     *
     * @param textureId the ID of the texture to be retrieved
     * @return an InputStream of the texture file
     */
    public InputStreamResource getInputStream(String textureId) {
        if (entity == null) {
            this.read(textureId);
        } else if (this.entity.getId() == null || !this.entity.getId().equals(textureId)) {
            this.read(textureId);
        }
        return new InputStreamResource(entity.getTextureFile().getInputStream());
    }

    /**
     * Generates the endpoint URL for streaming the texture by its ID.
     *
     * @param textureId the ID of the texture
     * @return the endpoint URL for streaming the texture
     */
    public String getTextureStreamEndpointUrl(String textureId) {
        return "/api/texture/" + textureId + "/stream";
    }

    /**
     * Generates the backend endpoint URL for accessing the texture file by its ID.
     *
     * @param textureId the ID of the texture
     * @return the backend endpoint URL for the texture file
     */
    public String getTextureFileBeEndpointUrl(String textureId) {
        return IApiClient.getLocalBaseBeUrl() + "texture" + "/download/" + textureId;
    }

    /**
     * Validates the create entity.
     *
     * @param createEntity Entity to validate
     * @return Validated entity
     * @throws RuntimeException if validation fails
     */
    @Override
    protected TextureEntity validateCreateEntity(TextureEntity createEntity) throws RuntimeException {
        if (createEntity.getTextureFile().isEmpty()) {
            throw new ApplicationContextException("Soubor pro nahrání textury nesmí být prázdný.");
        }
        if (createEntity.getTextureFile().getDisplayName().isEmpty()) {
            throw new ApplicationContextException("Název textury nesmí být prázdný.");
        }
        return createEntity;
    }

    /**
     * Creates the final entity from the create entity.
     *
     * @param createEntity Entity to create
     * @return Final entity
     * @throws RuntimeException if creation fails
     */
    @Override
    protected TextureEntity createFinalEntity(TextureEntity createEntity) throws RuntimeException {
        String csvString = createEntity.getCsvContent() == null ? "" : createEntity.getCsvContent();
        TextureEntity texture =  TextureEntity.builder()
                .name(createEntity.getTextureFile().getDisplayName())
                .created(Instant.now())
                .csvContent(csvString)
                .build();
        return TextureUpload.builder()
                .modelId(createEntity.getModelId())
                .textureFile(createEntity.getTextureFile())
                .texture(texture)
                .isPrimary(createEntity.getIsPrimary())
                .build();
    }
}
