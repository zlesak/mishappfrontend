package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.ModelApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileEntityRecursive;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileEntityTree;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public String saveFromUpload(
            String existingMetadataId,
            String name,
            InputStreamMultipartFile modelFile,
            InputStreamMultipartFile mainTextureFile,
            List<InputStreamMultipartFile> otherTextureFiles,
            List<InputStreamMultipartFile> csvFiles,
            String thumbnailDataUrl
    ) {
        boolean hasMainTexture = mainTextureFile != null;
        boolean hasOtherTextures = otherTextureFiles != null && !otherTextureFiles.isEmpty();
        boolean hasTextures = hasMainTexture || hasOtherTextures;

        final ModelEntity.ModelEntityBuilder<?, ?> builder = ModelEntity.builder()
                .name(name.trim())
                .inputStreamMultipartFile(modelFile)
                .isAdvanced(hasTextures)
                .description(thumbnailDataUrl);

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

    private static String toCsvName(String textureName) {
        if (textureName == null) return "texture.csv";
        int dot = textureName.lastIndexOf('.');
        return (dot > 0 ? textureName.substring(0, dot) : textureName) + ".csv";
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
}
