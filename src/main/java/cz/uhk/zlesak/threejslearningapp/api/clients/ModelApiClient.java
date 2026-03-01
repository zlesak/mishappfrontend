package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.*;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import cz.uhk.zlesak.threejslearningapp.exceptions.ApiCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * ModelApiClient provides connection to the backend service for managing models.
 * Overrides {@link #create} and {@link #update} to use multipart requests matching the
 * backend's {@code /create} and {@code /update} endpoints.
 */
@Slf4j
@Component
public class ModelApiClient extends AbstractApiClient<ModelEntity, QuickModelEntity, ModelFilter> {

    @Autowired
    public ModelApiClient(RestClient restClient, ObjectMapper objectMapper) {
        super(restClient, objectMapper, "model/");
    }

    @Override
    protected Class<ModelEntity> getEntityClass() {
        return ModelEntity.class;
    }

    @Override
    protected Class<QuickModelEntity> getQuicEntityClass() {
        return QuickModelEntity.class;
    }

    /**
     * Creates a new model with multipart file structure
     *
     * @param entity content to create
     * @return created model metadata as QuickModelEntity, deserialized from the backend's ModelIds response
     */
    @Override
    public QuickModelEntity create(ModelEntity entity) throws Exception {
        MultiValueMap<String, Object> body = buildMultipartBody(
                entity,
                buildInputFileDesc(entity),
                new ModelMetadata(entity.getDescription() != null ? entity.getDescription() : "", entity.isAdvanced())
        );
        return sendMultipartPost(baseUrl + "upload", body, "Chyba při nahrávání modelu", null);
    }

    /**
     * Updates an existing model
     *
     * @param id     of the entity to update
     * @param entity content to update the old saved data for
     * @return {@link ModelEntity} deserialized from the response.
     */
    @Override
    public ModelEntity update(String id, ModelEntity entity) throws Exception {
        MultiValueMap<String, Object> body = buildMultipartBody(
                entity,
                buildInputFileDesc(entity),
                new UpdateModelMetadata(id, entity.getDescription() != null ? entity.getDescription() : "", entity.isAdvanced())
        );
        QuickModelEntity quick = sendMultipartPut(baseUrl + "update", body, "Chyba při aktualizaci modelu", id);
        return ModelEntity.builder()
                .metadataId(quick.getMetadataId())
                .model(quick.getModel())
                .isAdvanced(quick.isAdvanced())
                .mainTexture(quick.getMainTexture())
                .otherTextures(quick.getOtherTextures())
                .build();
    }

    /**
     * Reads a single {@link ModelEntity} by its metadata ID.
     *
     * @param id of the entity
     * @return the converted ModelEntity
     */
    @Override
    public ModelEntity read(String id) throws Exception {
        FileEntityTree tree = sendGetRequest(baseUrl + id, FileEntityTree.class, "Chyba při získávání FileEntityTree", id);
        if (tree == null) return null;
        return mapFileEntityTreeToModelEntity(tree, id);
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

    /**
     * Recursively converts a {@link FileEntityRecursive} tree to a {@link ModelFileEntity} tree.
     *
     * @param fr the FileEntityRecursive to convert
     * @return the converted ModelFileEntity
     */
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

    /**
     * Populates textures from the flat related-file list using {@link FileSenseType}.
     * CSV files linked to a texture are downloaded and attached.
     *
     * @param entity          ModelEntity to get the texture data from
     * @param allRelatedFiles to relate the proper textures with
     */
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

    /**
     * Builds a {@link QuickTextureEntity} from a {@link ModelFileEntity}, attaching CSV content
     * if a linked CSV file exists in the related list.
     *
     * @param f ModelEntity to get the texture data from
     * @return QuickTextureEntity with the texture data
     */
    private QuickTextureEntity buildQuickTexture(ModelFileEntity f) {
        QuickTextureEntity.QuickTextureEntityBuilder<?, ?> builder = QuickTextureEntity.builder()
                .id(f.getId())
                .name(f.getName());

        if (f.getRelated() != null) {
            for (ModelFileEntity child : f.getRelated()) {
                if (child != null && child.getSenseType() == FileSenseType.CSV_FILE) {
                    try {
                        String url = AbstractApiClient.getStreamBeEndpointUrl(child.getId());
                        var resp = sendGetRequestRaw(url, byte[].class, "Chyba při stahování CSV", child.getId(), true);
                        InputStreamMultipartFile csvFile = parseFileResponse(resp, "Chyba při zpracování CSV", child.getId());
                        if (csvFile != null) {
                            builder.csvContent(new String(csvFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8));
                        }
                    } catch (Throwable ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    break;
                }
            }
        }
        return builder.build();
    }

    /**
     * Builds the multipart body used for both create and update requests.
     *
     * @param entity        model entity supplying the files
     * @param inputFileDesc pre-built file descriptor tree
     * @param modelMetadata metadata object ({@link ModelMetadata} or {@link UpdateModelMetadata})
     * @return MultiValueMap with the file data
     */
    private MultiValueMap<String, Object> buildMultipartBody(
            ModelEntity entity,
            InputFileDesc inputFileDesc,
            Object modelMetadata
    ) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        body.add("metadata", new HttpEntity<>(objectMapper.writeValueAsString(inputFileDesc), jsonHeaders));
        body.add("modelMetadata", new HttpEntity<>(objectMapper.writeValueAsString(modelMetadata), jsonHeaders));

        addFilePart(body, entity.getInputStreamMultipartFile());
        if (entity.getFullMainTexture() != null && entity.getFullMainTexture().getTextureFile() != null) {
            addFilePart(body, entity.getFullMainTexture().getTextureFile());
        }
        if (entity.getFullOtherTextures() != null) {
            for (TextureEntity tex : entity.getFullOtherTextures()) {
                if (tex.getTextureFile() != null) addFilePart(body, tex.getTextureFile());
            }
        }
        if (entity.getCsvFiles() != null) {
            for (InputStreamMultipartFile csv : entity.getCsvFiles()) {
                addFilePart(body, csv);
            }
        }
        return body;
    }

    /**
     * Adds one file as part with the correct Content-Disposition filename.
     *
     * @param body to attach the files to
     * @param file to attach to the body
     */
    private void addFilePart(MultiValueMap<String, Object> body, InputStreamMultipartFile file) {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("files", file.getOriginalFilename());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        body.add("files", new HttpEntity<>(resource, headers));
    }

    /**
     * Builds the {@link InputFileDesc} tree that describes the uploaded files and their hierarchy.
     * CSV files are matched to their parent texture by comparing base names.
     *
     * @param entity entity to build the result from
     * @return InputFileDesc with the descibed data
     */
    private InputFileDesc buildInputFileDesc(ModelEntity entity) {
        List<InputFileDesc> relatedFiles = new ArrayList<>();

        if (entity.getFullMainTexture() != null && entity.getFullMainTexture().getTextureFile() != null) {
            InputStreamMultipartFile f = entity.getFullMainTexture().getTextureFile();
            relatedFiles.add(new InputFileDesc(f.getOriginalFilename(), f.getDisplayName(), "", FileSenseType.MAIN_TEXTURE, List.of(), null));
        }

        if (entity.getFullOtherTextures() != null) {
            for (TextureEntity tex : entity.getFullOtherTextures()) {
                if (tex.getTextureFile() == null) continue;
                InputStreamMultipartFile tf = tex.getTextureFile();
                List<InputFileDesc> csvDescs = new ArrayList<>();
                if (entity.getCsvFiles() != null) {
                    for (InputStreamMultipartFile csv : entity.getCsvFiles()) {
                        if (toJpgName(csv.getOriginalFilename()).equals(tf.getOriginalFilename())) {
                            csvDescs.add(new InputFileDesc(csv.getOriginalFilename(), csv.getOriginalFilename(), "", FileSenseType.CSV_FILE, List.of(), null));
                        }
                    }
                }
                relatedFiles.add(new InputFileDesc(tf.getOriginalFilename(), tf.getDisplayName(), "", FileSenseType.OTHER_TEXTURE, csvDescs, null));
            }
        }

        InputStreamMultipartFile modelFile = entity.getInputStreamMultipartFile();
        return new InputFileDesc(
                modelFile.getOriginalFilename(),
                entity.getName(),
                entity.getDescription() != null ? entity.getDescription() : "",
                FileSenseType.MODEL,
                relatedFiles,
                null
        );
    }

    /**
     * Converts a filename to the same base name with a {@code .jpg} extension (mirrors the form logic).
     *
     * @param filename name of the file to be transformed
     * @return String with the transformed name
     */
    private static String toJpgName(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base + ".jpg";
    }

    /**
     * Downloads a single file by its GridFS ID.
     * Uses no Accept header so the server returns raw binary instead of JSON.
     *
     * @param fileId GridFS ID of the file to download
     * @return the file as {@link InputStreamMultipartFile}
     */
    public InputStreamMultipartFile downloadFile(String fileId) throws Exception {
        String url = getStreamBeEndpointUrl(fileId);
        var resp = sendGetRequestRaw(url, byte[].class, "Chyba při stahování souboru", fileId, false);
        return parseFileResponse(resp, "Chyba při zpracování souboru", fileId);
    }

    private QuickModelEntity sendMultipartPost(String url, MultiValueMap<String, Object> body, String errorMessage, String entityId) throws Exception {
        String token = getJwtToken();
        try {
            var spec = restClient.post().uri(url).contentType(MediaType.MULTIPART_FORM_DATA);
            if (token != null) spec = spec.headers(h -> h.setBearerAuth(token));
            String json = spec.body(body).retrieve().body(String.class);
            return objectMapper.readValue(json, QuickModelEntity.class);
        } catch (HttpStatusCodeException ex) {
            throw new ApiCallException(errorMessage, entityId, url, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (Exception e) {
            throw new Exception("Neočekávaná chyba při volání API: " + errorMessage + " - " + e.getMessage(), e);
        }
    }

    private QuickModelEntity sendMultipartPut(String url, MultiValueMap<String, Object> body, String errorMessage, String entityId) throws Exception {
        String token = getJwtToken();
        try {
            var spec = restClient.put().uri(url).contentType(MediaType.MULTIPART_FORM_DATA);
            if (token != null) spec = spec.headers(h -> h.setBearerAuth(token));
            String json = spec.body(body).retrieve().body(String.class);
            return objectMapper.readValue(json, QuickModelEntity.class);
        } catch (HttpStatusCodeException ex) {
            throw new ApiCallException(errorMessage, entityId, url, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (Exception e) {
            throw new Exception("Neočekávaná chyba při volání API: " + errorMessage + " - " + e.getMessage(), e);
        }
    }
}
