package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.BinaryFileStore;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.InputFileDesc;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores the files a 3D model is made of and hands back the structure describing them.
 * Binaries go to GridFS; the structure is kept with the model, so there is no separate file
 * collection to keep in step.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    /**
     * Deepest file hierarchy accepted on upload. The real structure is model to texture to CSV, so
     * anything deeper is a malformed descriptor rather than legitimate nesting.
     */
    private static final int MAX_DEPTH = 8;

    private final BinaryFileStore binaryFileStore;

    /**
     * Stores an uploaded file hierarchy. A file referenced from several branches is uploaded once
     * and reused, so a shared texture is not duplicated.
     *
     * @param files    uploaded parts keyed by their original filename.
     * @param metadata descriptor tree naming those parts and their relationships.
     * @return the stored hierarchy, with ids assigned.
     */
    public ModelFileEntity store(Map<String, MultipartFile> files, InputFileDesc metadata) {
        return store(files, metadata, new LinkedHashMap<>(), new LinkedHashSet<>(), 0);
    }

    private ModelFileEntity store(Map<String, MultipartFile> files,
                                  InputFileDesc metadata,
                                  Map<String, String> uploaded,
                                  Set<String> ancestry,
                                  int depth) {
        if (metadata == null) {
            throw new BackendException.Validation("Popis nahrávaného souboru chybí.");
        }
        if (depth > MAX_DEPTH) {
            throw new BackendException.Validation("Struktura nahrávaných souborů je příliš zanořená.");
        }

        String originalName = metadata.getOriginalFileName();
        MultipartFile file = files.get(originalName);
        if (file == null) {
            throw new BackendException.Validation("Soubor " + originalName + " nebyl nalezen mezi nahranými soubory.");
        }
        if (!ancestry.add(originalName)) {
            throw new BackendException.Validation("Struktura nahrávaných souborů obsahuje cyklus u " + originalName + ".");
        }

        String fileId = uploaded.computeIfAbsent(originalName, ignored -> binaryFileStore.store(file));

        List<ModelFileEntity> related = new ArrayList<>();
        if (metadata.getRelatedFiles() != null) {
            for (InputFileDesc child : metadata.getRelatedFiles()) {
                related.add(store(files, child, uploaded, ancestry, depth + 1));
            }
        }
        ancestry.remove(originalName);

        return ModelFileEntity.builder()
                .id(fileId)
                .name(metadata.getName())
                .senseType(metadata.getFileSenseType())
                .related(related)
                .build();
    }

    /**
     * Removes a file and everything below it. Missing entries are ignored so cleaning up a
     * partially deleted model still converges.
     *
     * @param root root of the hierarchy, may be {@code null}.
     */
    public void deleteTree(ModelFileEntity root) {
        if (root == null) {
            return;
        }
        if (root.getRelated() != null) {
            root.getRelated().forEach(this::deleteTree);
        }
        try {
            binaryFileStore.delete(root.getId());
        } catch (RuntimeException e) {
            log.warn("Soubor {} se nepodařilo smazat: {}", root.getId(), e.getMessage());
        }
    }

    /**
     * @param fileId id of the file.
     * @return whether the file is stored.
     */
    public boolean exists(String fileId) {
        return fileId != null && !fileId.isBlank() && binaryFileStore.exists(fileId);
    }

    /**
     * @param fileId id of the file.
     * @return the stored file.
     * @throws BackendException.NotFound when nothing is stored under that id.
     */
    public GridFsResource requireResource(String fileId) {
        GridFsResource resource = binaryFileStore.find(fileId);
        if (resource == null) {
            throw new BackendException.NotFound("Soubor s ID " + fileId + " nebyl nalezen.");
        }
        return resource;
    }

    /**
     * Reads a stored text file, used to pull a texture's CSV annotation into the model.
     *
     * @param fileId id of the file.
     * @return the file's text, or {@code null} when it cannot be read.
     */
    public String readText(String fileId) {
        try {
            GridFsResource resource = binaryFileStore.find(fileId);
            if (resource == null) {
                return null;
            }
            try (var input = resource.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Obsah souboru {} se nepodařilo přečíst: {}", fileId, e.getMessage());
            return null;
        }
    }

    /**
     * @param root  root of a stored hierarchy.
     * @param types roles to look for.
     * @return the files below the root that play one of the given roles.
     */
    public static List<ModelFileEntity> childrenOfType(ModelFileEntity root, FileSenseType... types) {
        List<ModelFileEntity> found = new ArrayList<>();
        if (root == null || root.getRelated() == null) {
            return found;
        }
        Set<FileSenseType> wanted = Set.of(types);
        for (ModelFileEntity child : root.getRelated()) {
            if (child != null && wanted.contains(child.getSenseType())) {
                found.add(child);
            }
        }
        return found;
    }
}
