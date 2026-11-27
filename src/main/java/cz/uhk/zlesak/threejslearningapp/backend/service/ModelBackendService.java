package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ModelRepository;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.MongoCollections;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.VersionedWrites;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.InputFileDesc;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns 3D models: their upload, the files behind them and the textures the viewer needs.
 * Textures are worked out once at upload time and stored with the model, so displaying a model
 * costs a single read.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelBackendService {

    private final ModelRepository modelRepository;
    private final FileStorageService fileStorageService;
    private final ListingQueries listingQueries;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLog auditLog;
    private final VersionedWrites versionedWrites;
    /** Chapters reference models and deleting a model has to check them; the lazy lookup breaks the cycle. */
    private final ObjectProvider<ChapterBackendService> chapterLookup;

    /**
     * Stores a new model with its textures and annotation files.
     *
     * @param files       uploaded parts.
     * @param metadata    descriptor tree; its root must describe the model file itself.
     * @param description presentation metadata for the UI (thumbnail, background).
     * @param advanced    whether the model uses per-area textures.
     * @return the stored model.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public QuickModelEntity upload(List<MultipartFile> files,
                                   InputFileDesc metadata,
                                   String description,
                                   boolean advanced) {
        requireModelRoot(metadata);
        ModelFileEntity root = fileStorageService.store(index(files), metadata);

        Instant now = VersionedWrites.now();
        QuickModelEntity saved = modelRepository.save(QuickModelEntity.builder()
                .name(root.getName())
                .creatorId(currentUserProvider.requireUserId())
                .creatorName(currentUserProvider.currentUserName())
                .description(description)
                .model(root)
                .isAdvanced(advanced)
                .mainTexture(mainTextureOf(root))
                .otherTextures(otherTexturesOf(root))
                .created(now)
                .updated(now)
                .build());
        auditLog.success(AuditLog.Action.CREATE, "model", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * Replaces the files of an existing model.
     * The new files are stored and the model switched over before the old files are removed, so a
     * failure part way through leaves the model readable rather than pointing at deleted files.
     *
     * <p>The switch only happens if nobody else has saved the model since the editor loaded it:
     * {@code basedOn} must still be the stored {@code updated}. A stale save is turned away before
     * any file is stored when that is already visible, and otherwise by the conditional write
     * itself; in that case the files it stored are removed again, and the stored version keeps its
     * own. Only the files of the version actually replaced are ever deleted.
     *
     * @param modelId     id of the model to replace.
     * @param files       uploaded parts.
     * @param metadata    descriptor tree for the new files.
     * @param description presentation metadata for the UI.
     * @param advanced    whether the model uses per-area textures.
     * @param basedOn     {@code updated} of the version the edit started from.
     * @return the updated model.
     * @throws BackendException.Conflict when the model was saved by someone else in the meantime.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public QuickModelEntity update(String modelId,
                                   List<MultipartFile> files,
                                   InputFileDesc metadata,
                                   String description,
                                   boolean advanced,
                                   Instant basedOn) {
        requireModelRoot(metadata);
        QuickModelEntity existing = require(modelId);
        currentUserProvider.requireAuthorOrTeacher(existing.getCreatorId(),
                "Model může upravit pouze jeho autor.");
        if (!VersionedWrites.sameVersion(existing.getUpdated(), basedOn)) {
            throw conflict(existing);
        }

        ModelFileEntity root = fileStorageService.store(index(files), metadata);

        existing.setName(root.getName());
        existing.setDescription(description);
        existing.setModel(root);
        existing.setAdvanced(advanced);
        existing.setMainTexture(mainTextureOf(root));
        existing.setOtherTextures(otherTexturesOf(root));
        existing.setUpdated(VersionedWrites.now());

        QuickModelEntity replaced;
        try {
            replaced = versionedWrites.replaceIfUnchanged(existing, basedOn, QuickModelEntity.class, MongoCollections.MODEL);
        } catch (RuntimeException e) {
            fileStorageService.deleteTree(root);
            throw e;
        }
        if (replaced == null) {
            // Someone saved between the check above and the write. Their version is the stored one
            // and keeps its files; the ones stored for this attempt belong to nothing.
            fileStorageService.deleteTree(root);
            throw conflict(require(modelId));
        }

        fileStorageService.deleteTree(replaced.getModel());
        auditLog.success(AuditLog.Action.UPDATE, "model", existing.getId(), existing.getName());
        return existing;
    }

    /**
     * Renames a model and keeps its stored files exactly as they are: nothing is uploaded and nothing
     * in GridFS changes. This is what saving from the conflict dialog does when the user keeps the
     * stored version's files.
     *
     * @param modelId id of the model.
     * @param name    the new name.
     * @param basedOn {@code updated} of the version whose files are being kept.
     * @return the updated model.
     * @throws BackendException.Conflict when the model was saved by someone else in the meantime,
     *                                   in which case the files being "kept" are no longer the stored ones.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public QuickModelEntity renameKeepingFiles(String modelId, String name, Instant basedOn) {
        if (name == null || name.isBlank()) {
            throw new BackendException.Validation("Název modelu nesmí být prázdný.");
        }
        QuickModelEntity existing = require(modelId);
        currentUserProvider.requireAuthorOrTeacher(existing.getCreatorId(),
                "Model může upravit pouze jeho autor.");
        if (!VersionedWrites.sameVersion(existing.getUpdated(), basedOn)) {
            throw conflict(existing);
        }

        existing.setName(name);
        if (existing.getModel() != null) {
            // The model file's entry carries the display name an upload gives it; only the document
            // changes here, the file in GridFS keeps its own name.
            existing.getModel().setName(name);
        }
        existing.setUpdated(VersionedWrites.now());

        if (versionedWrites.replaceIfUnchanged(existing, basedOn, QuickModelEntity.class, MongoCollections.MODEL) == null) {
            throw conflict(require(modelId));
        }
        auditLog.success(AuditLog.Action.UPDATE, "model", existing.getId(), existing.getName());
        return existing;
    }

    private BackendException.Conflict conflict(QuickModelEntity current) {
        auditLog.failure(AuditLog.Action.UPDATE, "model", current.getId(), "conflict");
        return new BackendException.Conflict(
                "Model mezitím uložil někdo jiný. Vaše změny nebyly uloženy.", current);
    }

    /**
     * @param modelId id of the model.
     * @return the model.
     * @throws BackendException.NotFound when no such model exists.
     */
    public QuickModelEntity require(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new BackendException.Validation("ID modelu nesmí být prázdné.");
        }
        return modelRepository.findById(modelId)
                .orElseThrow(() -> new BackendException.NotFound("Model s ID " + modelId + " nebyl nalezen."));
    }

    /**
     * @param modelIds ids to load.
     * @return the models that exist, in the order the ids were given.
     */
    public List<QuickModelEntity> findAll(List<String> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) {
            return List.of();
        }

        Map<String, QuickModelEntity> byId = new LinkedHashMap<>();
        modelRepository.findAllById(modelIds).forEach(model -> byId.put(model.getId(), model));

        List<QuickModelEntity> ordered = new ArrayList<>();
        for (String modelId : modelIds) {
            QuickModelEntity model = byId.get(modelId);
            if (model != null) {
                ordered.add(model);
            }
        }
        return ordered;
    }

    /**
     * Removes a model together with every file it owns.
     *
     * @param modelId id of the model.
     */
    @PreAuthorize("hasRole('CREATE_CHAPTER')")
    public void delete(String modelId) {
        QuickModelEntity model = require(modelId);
        currentUserProvider.requireAuthorOrTeacher(model.getCreatorId(),
                "Model může smazat pouze jeho autor.");

        List<String> usedBy = chapterLookup.getObject().usingModel(modelId).stream()
                .map(chapter -> chapter.getName() == null ? chapter.getId() : chapter.getName())
                .toList();
        if (!usedBy.isEmpty()) {
            throw new BackendException.Validation(
                    "Model nelze smazat, používají jej kapitoly: " + String.join(", ", usedBy) + ".");
        }

        fileStorageService.deleteTree(model.getModel());
        modelRepository.deleteById(modelId);
        auditLog.success(AuditLog.Action.DELETE, "model", modelId, model.getName());
    }

    /**
     * @param filterParameters paging and filtering requested by the UI.
     * @return one page of models.
     */
    public PageResult<QuickModelEntity> list(FilterParameters<ModelFilter> filterParameters) {
        return listingQueries.page(
                listingQueries.baseQuery(filterParameters.getFilter()),
                filterParameters.getPageRequest(),
                QuickModelEntity.class,
                MongoCollections.MODEL);
    }

    /**
     * Finds the models whose name contains the given text.
     *
     * <p>Used to filter chapters by the model they contain: a chapter document stores only model ids,
     * so the name the user typed has to become a set of ids before the chapter query can run.
     *
     * @param namePart part of a model name, matched case-insensitively.
     * @return ids of the matching models, empty when nothing matches or nothing was asked for.
     */
    public List<String> idsByName(String namePart) {
        return listingQueries.modelIdsByName(namePart);
    }

    private void requireModelRoot(InputFileDesc metadata) {
        if (metadata == null || metadata.getFileSenseType() != FileSenseType.MODEL) {
            throw new BackendException.Validation("Kořenový soubor musí být 3D model.");
        }
    }

    private Map<String, MultipartFile> index(List<MultipartFile> files) {
        Map<String, MultipartFile> byName = new LinkedHashMap<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getOriginalFilename() != null) {
                    byName.putIfAbsent(file.getOriginalFilename(), file);
                }
            }
        }
        return byName;
    }

    private QuickTextureEntity mainTextureOf(ModelFileEntity root) {
        return FileStorageService.childrenOfType(root, FileSenseType.MAIN_TEXTURE).stream()
                .findFirst()
                .map(texture -> toTexture(texture, root.getId(), true))
                .orElse(null);
    }

    private List<QuickTextureEntity> otherTexturesOf(ModelFileEntity root) {
        return FileStorageService.childrenOfType(root, FileSenseType.OTHER_TEXTURE).stream()
                .map(texture -> toTexture(texture, root.getId(), false))
                .toList();
    }

    /**
     * Builds the viewer's view of one texture, pulling in the CSV that maps its colours to areas.
     */
    private QuickTextureEntity toTexture(ModelFileEntity texture, String modelFileId, boolean primary) {
        String csvContent = FileStorageService.childrenOfType(texture, FileSenseType.CSV_FILE).stream()
                .findFirst()
                .map(csv -> fileStorageService.readText(csv.getId()))
                .orElse(null);

        return QuickTextureEntity.builder()
                .id(texture.getId())
                .name(texture.getName())
                .textureFileId(texture.getId())
                .modelId(modelFileId)
                .isPrimary(primary)
                .csvContent(csvContent)
                .build();
    }
}
