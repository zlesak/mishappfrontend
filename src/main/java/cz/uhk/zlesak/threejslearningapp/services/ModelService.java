package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.uhk.zlesak.threejslearningapp.api.clients.AbstractApiClient;
import cz.uhk.zlesak.threejslearningapp.api.clients.ModelApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.model.*;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing 3D models, including uploading and retrieving model files and textures.
 * Model uploads (including textures and CSV files) are handled in a single API call via ModelApiClient.
 *
 * @see ModelApiClient
 */
@Service
@Slf4j
@Scope("prototype")
public class ModelService extends AbstractService<ModelEntity, QuickModelEntity, ModelFilter> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DESCRIPTION_THUMBNAIL_KEY = "thumbnailDataUrl";
    private static final String DESCRIPTION_BACKGROUND_KEY = "background";
    private static final int MAX_PERSISTED_DESCRIPTION_LENGTH = 250_000;
    private static final String DESCRIPTION_BACKGROUND_SPEC_KEY = "backgroundSpec";
    private static final String DESCRIPTION_BACKGROUND_SPEC_JSON_KEY = "backgroundSpecJson";

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
     * Reads a model entity by its metadata ID.
     * Fetches the file entity tree from the backend and maps it to a {@link ModelEntity}.
     *
     * @param entityId metadata ID of the model.
     * @return ModelEntity with populated file references and textures.
     * @throws RuntimeException if the API call fails.
     */
    @Override
    public ModelEntity read(String entityId) throws RuntimeException {
        try {
            if (entityId == null || entityId.isEmpty()) {
                throw new RuntimeException("ID entity nesmí být prázdné.");
            }

            if (entity == null || entity.getId() == null || !entity.getId().equals(entityId)) {
                FileEntityTree tree = ((ModelApiClient) apiClient).readFileEntityTree(entityId);
                entity = tree == null ? null : mapFileEntityTreeToModelEntity(tree, entityId);
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Chyba při získávání entity: " + e.getMessage(), e);
        }
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

    /**
     * Downloads all files associated with a model entity and packages them for form pre-filling.
     *
     * @param modelEntity model entity whose files should be downloaded.
     * @return ModelPrefillData containing the model file, textures, and CSV files.
     * @throws Exception if any file download fails.
     */
    public ModelPrefillData buildPrefillData(ModelEntity modelEntity) throws Exception {
        InputStreamMultipartFile modelFile = downloadFile(modelEntity.getModel().getId());

        InputStreamMultipartFile mainTexture = null;
        if (modelEntity.getMainTexture() != null) {
            try {
                mainTexture = downloadFile(modelEntity.getMainTexture().getId());
                mainTexture.setDisplayName(modelEntity.getMainTexture().getName());
            } catch (Exception ignored) {
            }
        }

        List<InputStreamMultipartFile> otherTextures = new ArrayList<>();
        List<InputStreamMultipartFile> csvFiles = new ArrayList<>();

        if (modelEntity.getOtherTextures() != null) {
            for (QuickTextureEntity tex : modelEntity.getOtherTextures()) {
                try {
                    InputStreamMultipartFile ot = downloadFile(tex.getId());
                    ot.setDisplayName(tex.getName());
                    otherTextures.add(ot);
                    if (tex.getCsvContent() != null && !tex.getCsvContent().isEmpty()) {
                        String csvName = toCsvName(ot.getOriginalFilename());
                        csvFiles.add(new InputStreamMultipartFile(
                                new java.io.ByteArrayInputStream(tex.getCsvContent().getBytes(StandardCharsets.UTF_8)),
                                csvName, csvName));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return new ModelPrefillData(
                modelFile,
                mainTexture,
                otherTextures.isEmpty() ? null : otherTextures,
                csvFiles.isEmpty() ? null : csvFiles
        );
    }

    /**
     * Creates or updates a model from uploaded files.
     * Determines create vs update based on whether {@code existingMetadataId} is present.
     *
     * @param existingMetadataId existing model metadata ID for update, or {@code null} for create.
     * @param name               model display name.
     * @param modelFile          uploaded 3D model file.
     * @param mainTextureFile    optional main texture file.
     * @param otherTextureFiles  optional list of additional texture files.
     * @param csvFiles           optional list of CSV annotation files.
     * @param thumbnailDataUrl   base64-encoded thumbnail data URL.
     * @param backgroundSpecJson JSON describing the background spec.
     * @return saved model metadata ID.
     */
    public String saveFromUpload(
            String existingMetadataId,
            String name,
            InputStreamMultipartFile modelFile,
            InputStreamMultipartFile mainTextureFile,
            List<InputStreamMultipartFile> otherTextureFiles,
            List<InputStreamMultipartFile> csvFiles,
            String thumbnailDataUrl,
            String backgroundSpecJson
    ) {
        boolean hasMainTexture = mainTextureFile != null;
        boolean hasOtherTextures = otherTextureFiles != null && !otherTextureFiles.isEmpty();
        boolean hasTextures = hasMainTexture || hasOtherTextures;

        InputStreamMultipartFile backgroundImageFile = toBackgroundImageFile(backgroundSpecJson);
        String persistedBackgroundSpecJson = selectBackgroundSpecForDescriptionPersistence(existingMetadataId, backgroundSpecJson);

        final ModelEntity.ModelEntityBuilder<?, ?> builder = ModelEntity.builder()
                .name(name.trim())
                .inputStreamMultipartFile(modelFile)
                .isAdvanced(hasTextures)
                .description(encodeModelDescription(thumbnailDataUrl, persistedBackgroundSpecJson))
                .backgroundImageFile(backgroundImageFile);

        if (hasMainTexture) {
            builder.fullMainTexture(TextureEntity.builder().textureFile(mainTextureFile).build());
        }

        if (hasOtherTextures) {
            builder.fullOtherTextures(
                    otherTextureFiles.stream()
                            .map(file -> TextureEntity.builder().textureFile(file).build())
                            .collect(Collectors.toList())
            );
        }

        if (csvFiles != null && !csvFiles.isEmpty()) {
            builder.csvFiles(csvFiles);
        }

        if (existingMetadataId != null && !existingMetadataId.isBlank()) {
            return update(existingMetadataId, builder.build());
        }
        return create(builder.build());
    }

    private String selectBackgroundSpecForDescriptionPersistence(String existingMetadataId, String requestedBackgroundSpecJson) {
        String requested = requestedBackgroundSpecJson == null ? "" : requestedBackgroundSpecJson.trim();
        if (requested.isEmpty()) {
            return "";
        }

        String requestedType = extractBackgroundType(requested);
        if (!"image".equalsIgnoreCase(requestedType)) {
            return requested;
        }
        return "";
    }

    private String extractBackgroundType(String backgroundSpecJson) {
        if (backgroundSpecJson == null || backgroundSpecJson.isBlank()) {
            return "";
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(backgroundSpecJson);
            return node.path("type").asText("").trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String toCsvName(String textureName) {
        if (textureName == null) return "texture.csv";
        int dot = textureName.lastIndexOf('.');
        return (dot > 0 ? textureName.substring(0, dot) : textureName) + ".csv";
    }

    /**
     * Encodes model UI metadata (thumbnail + background) into description.
     * Falls back to legacy plain thumbnail string when no background is provided.
     */
    public String encodeModelDescription(String thumbnailDataUrl, String backgroundSpecJson) {
        String safeThumbnail = thumbnailDataUrl == null ? "" : thumbnailDataUrl.trim();
        String safeBackground = sanitizeBackgroundForPersistence(backgroundSpecJson);

        if (safeBackground.isEmpty()) {
            return safeThumbnail;
        }

        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();
            root.put(DESCRIPTION_THUMBNAIL_KEY, safeThumbnail);
            JsonNode backgroundNode = OBJECT_MAPPER.readTree(safeBackground);
            root.set(DESCRIPTION_BACKGROUND_KEY, backgroundNode);
            String encoded = OBJECT_MAPPER.writeValueAsString(root);
            if (encoded.length() > MAX_PERSISTED_DESCRIPTION_LENGTH) {
                log.warn("Encoded model description is too large ({} chars), storing thumbnail only.", encoded.length());
                return safeThumbnail;
            }
            return encoded;
        } catch (Exception e) {
            log.warn("Could not encode background metadata into model description: {}", e.getMessage());
            return safeThumbnail;
        }
    }

    private String sanitizeBackgroundForPersistence(String backgroundSpecJson) {
        String safeBackground = backgroundSpecJson == null ? "" : backgroundSpecJson.trim();
        if (safeBackground.isEmpty()) {
            return "";
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(safeBackground);
            String type = node.path("type").asText("");
            JsonNode valueNode = node.path("value");

            if ("image".equalsIgnoreCase(type) && valueNode.isTextual()) {
                String value = valueNode.asText("");
                if (value.startsWith("data:")) {
                    return "";
                }
            }

            if (safeBackground.length() > 20_000) {
                return "";
            }

            return safeBackground;
        } catch (Exception e) {
            log.warn("Invalid background spec JSON, skipping persistence: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Parses description that may be either legacy thumbnail string or JSON metadata.
     */
    public ModelDescriptionData parseModelDescription(String description) {
        if (description == null || description.isBlank()) {
            return new ModelDescriptionData("", null);
        }

        String trimmed = description.trim();
        if (!trimmed.startsWith("{")) {
            return new ModelDescriptionData(trimmed, null);
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmed);
            String thumbnail = root.path(DESCRIPTION_THUMBNAIL_KEY).asText("");
            JsonNode backgroundNode = resolveBackgroundNode(root);
            String background = normalizeBackgroundNodeToJson(backgroundNode);
            return new ModelDescriptionData(thumbnail, background);
        } catch (Exception e) {
            log.warn("Could not parse model description metadata, using legacy thumbnail fallback: {}", e.getMessage());
            return new ModelDescriptionData(trimmed, null);
        }
    }

    /**
     * Extracts the thumbnail data URL from a model description string.
     *
     * @param description encoded model description.
     * @return thumbnail data URL, or empty string if absent.
     */
    public String extractThumbnailDataUrl(String description) {
        return parseModelDescription(description).thumbnailDataUrl();
    }

    /**
     * Extracts the background spec JSON from a model description string.
     *
     * @param description encoded model description.
     * @return background spec JSON, or {@code null} if absent.
     */
    public String extractBackgroundSpecJson(String description) {
        return parseModelDescription(description).backgroundSpecJson();
    }

    /**
     * Resolves the effective background spec JSON for a model entity.
     * Prefers a background image stored as a GridFS file; falls back to description metadata.
     *
     * @param entity model entity to resolve background for.
     * @return background spec JSON, or {@code null} if none is available.
     */
    public String resolveBackgroundSpecJson(ModelEntity entity) {
        if (entity == null) {
            return null;
        }

        String backgroundFileId = findBackgroundImageFileId(entity);
        if (backgroundFileId != null && !backgroundFileId.isBlank()) {
            String streamUrl = AbstractApiClient.getStreamBeEndpointUrl(backgroundFileId);
            try {
                ObjectNode root = OBJECT_MAPPER.createObjectNode();
                root.put("type", "image");
                root.put("value", streamUrl);
                return OBJECT_MAPPER.writeValueAsString(root);
            } catch (Exception e) {
                log.warn("ModelService.resolveBackgroundSpecJson: could not serialize resolved background image spec: {}", e.getMessage());
            }
        }

        String fromDescription = extractBackgroundSpecJson(entity.getDescription());
        if (fromDescription != null && !fromDescription.isBlank()) {
            return fromDescription;
        }

        return null;
    }

    private String findBackgroundImageFileId(ModelEntity entity) {
        if (entity.getModel() == null || entity.getModel().getRelated() == null) {
            return null;
        }

        for (ModelFileEntity related : entity.getModel().getRelated()) {
            if (related != null && related.getSenseType() == FileSenseType.BACKGROUND_IMAGE) {
                return related.getId();
            }
        }
        return null;
    }

    private InputStreamMultipartFile toBackgroundImageFile(String backgroundSpecJson) {
        if (backgroundSpecJson == null || backgroundSpecJson.isBlank()) {
            return null;
        }

        try {
            JsonNode backgroundNode = OBJECT_MAPPER.readTree(backgroundSpecJson);
            String type = backgroundNode.path("type").asText("");
            if (!"image".equalsIgnoreCase(type)) {
                return null;
            }

            String value = backgroundNode.path("value").asText("");
            if (!value.startsWith("data:")) {
                String existingFileId = extractModelDownloadFileId(value);
                if (existingFileId == null) {
                    return null;
                }

                try {
                    InputStreamMultipartFile existingBackground = downloadFile(existingFileId);
                    if (existingBackground != null && !existingBackground.isEmpty()) {
                        String originalName = existingBackground.getOriginalFilename() != null
                                ? existingBackground.getOriginalFilename()
                                : "background-image.jpg";
                        return InputStreamMultipartFile.builder()
                                .inputStream(new ByteArrayInputStream(existingBackground.getBytes()))
                                .fileName(originalName)
                                .displayName("background-image")
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("Could not re-download existing background image file {} for update: {}", existingFileId, e.getMessage());
                }

                return null;
            }

            int comma = value.indexOf(',');
            if (comma <= 0 || comma >= value.length() - 1) {
                return null;
            }

            String metadata = value.substring(5, comma);
            String base64Data = value.substring(comma + 1);
            if (!metadata.contains(";base64")) {
                return null;
            }

            String mimeType = metadata.substring(0, metadata.indexOf(';')).trim();
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            String extension = extensionForMimeType(mimeType);
            String fileName = "background-image." + extension;

            return InputStreamMultipartFile.builder()
                    .inputStream(new ByteArrayInputStream(bytes))
                    .fileName(fileName)
                    .displayName("background-image")
                    .build();
        } catch (Exception e) {
            log.warn("Could not convert background image data URL to multipart file: {}", e.getMessage());
            return null;
        }
    }

    private String extensionForMimeType(String mimeType) {
        if (mimeType == null) {
            return "jpg";
        }
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
    }

    private String extractModelDownloadFileId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String marker = "/api/model/download/";
        int idx = url.indexOf(marker);
        if (idx < 0) {
            return null;
        }

        String rest = url.substring(idx + marker.length());
        int end = rest.indexOf('?');
        if (end >= 0) {
            rest = rest.substring(0, end);
        }
        end = rest.indexOf('/');
        if (end >= 0) {
            rest = rest.substring(0, end);
        }

        String trimmed = rest.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private JsonNode resolveBackgroundNode(JsonNode root) {
        JsonNode backgroundNode = root.get(DESCRIPTION_BACKGROUND_KEY);
        if (backgroundNode != null && !backgroundNode.isNull()) {
            return backgroundNode;
        }

        JsonNode legacyBackgroundSpec = root.get(DESCRIPTION_BACKGROUND_SPEC_KEY);
        if (legacyBackgroundSpec != null && !legacyBackgroundSpec.isNull()) {
            return legacyBackgroundSpec;
        }

        JsonNode legacyBackgroundSpecJson = root.get(DESCRIPTION_BACKGROUND_SPEC_JSON_KEY);
        if (legacyBackgroundSpecJson != null && !legacyBackgroundSpecJson.isNull()) {
            return legacyBackgroundSpecJson;
        }
        return null;
    }

    private String normalizeBackgroundNodeToJson(JsonNode backgroundNode) {
        if (backgroundNode == null || backgroundNode.isNull()) {
            return null;
        }

        try {
            if (backgroundNode.isObject()) {
                return OBJECT_MAPPER.writeValueAsString(backgroundNode);
            }

            if (backgroundNode.isTextual()) {
                String textValue = backgroundNode.asText("").trim();
                if (textValue.isEmpty()) {
                    return null;
                }

                if (textValue.startsWith("{")) {
                    JsonNode parsedTextNode = OBJECT_MAPPER.readTree(textValue);
                    if (parsedTextNode != null && parsedTextNode.isObject()) {
                        return OBJECT_MAPPER.writeValueAsString(parsedTextNode);
                    }
                }

                ObjectNode colorSpec = OBJECT_MAPPER.createObjectNode();
                colorSpec.put("type", "color");
                colorSpec.put("value", textValue);
                return OBJECT_MAPPER.writeValueAsString(colorSpec);
            }
        } catch (Exception e) {
            log.warn("Could not normalize background metadata: {}", e.getMessage());
        }

        return null;
    }

    private ModelEntity mapFileEntityTreeToModelEntity(FileEntityTree tree, String modelMetadataId) {
        List<ModelFileEntity> allRelatedFiles = new ArrayList<>();
        if (tree.getAllRelatedFiles() != null) {
            for (FileEntityRecursive fr : tree.getAllRelatedFiles()) {
                allRelatedFiles.add(convertRecursiveToModelFileEntity(fr));
            }
        }

        ModelFileEntity root = ModelFileEntity.builder()
                .id(tree.getId())
                .name(tree.getName())
                .senseType(tree.getSenseType())
                .related(allRelatedFiles)
                .build();

        ModelEntity model = ModelEntity.builder()
                .id(tree.getId())
                .model(root)
                .metadataId(modelMetadataId)
                .name(tree.getName())
                .creatorId(tree.getCreatorId())
                .description(tree.getDescription())
                .created(tree.getCreated())
                .updated(tree.getUpdated())
                .isAdvanced(tree.isAdvanced())
                .build();
        populateTextures(model, allRelatedFiles);
        return model;
    }

    private ModelFileEntity convertRecursiveToModelFileEntity(FileEntityRecursive fr) {
        ModelFileEntity mfe = new ModelFileEntity();
        mfe.setId(fr.getId());
        mfe.setName(fr.getName());
        mfe.setSenseType(fr.getSenseType());
        if (fr.getRelatedFiles() != null) {
            List<ModelFileEntity> nested = new ArrayList<>();
            for (FileEntityRecursive child : fr.getRelatedFiles()) {
                nested.add(convertRecursiveToModelFileEntity(child));
            }
            mfe.setRelated(nested);
        }
        return mfe;
    }

    private void populateTextures(ModelEntity entity, List<ModelFileEntity> allRelatedFiles) {
        List<QuickTextureEntity> others = new ArrayList<>();
        for (ModelFileEntity f : allRelatedFiles) {
            if (f == null) continue;
            if (f.getSenseType() == FileSenseType.MAIN_TEXTURE && entity.getMainTexture() == null) {
                entity.setMainTexture(buildQuickTexture(f));
            } else if (f.getSenseType() == FileSenseType.OTHER_TEXTURE) {
                others.add(buildQuickTexture(f));
            }
        }
        if (!others.isEmpty()) entity.setOtherTextures(others);
    }

    private QuickTextureEntity buildQuickTexture(ModelFileEntity textureFile) {
        QuickTextureEntity.QuickTextureEntityBuilder<?, ?> builder = QuickTextureEntity.builder()
                .id(textureFile.getId())
                .name(textureFile.getName());

        if (textureFile.getRelated() != null) {
            for (ModelFileEntity child : textureFile.getRelated()) {
                if (child != null && child.getSenseType() == FileSenseType.CSV_FILE) {
                    try {
                        InputStreamMultipartFile csvFile = downloadFile(child.getId());
                        if (csvFile != null) {
                            builder.csvContent(new String(csvFile.getBytes(), StandardCharsets.UTF_8));
                        }
                    } catch (Throwable throwable) {
                        log.error("Failed to download CSV file for texture {}: {}", textureFile.getName(), throwable.getMessage());
                        new ErrorNotification("Failed to download CSV file for texture " + textureFile.getName());
                    }
                    break;
                }
            }
        }
        return builder.build();
    }

    public record ModelPrefillData(
            InputStreamMultipartFile modelFile,
            InputStreamMultipartFile mainTexture,
            List<InputStreamMultipartFile> otherTextures,
            List<InputStreamMultipartFile> csvFiles
    ) {
    }

    public record ModelDescriptionData(String thumbnailDataUrl, String backgroundSpecJson) {
    }
}
