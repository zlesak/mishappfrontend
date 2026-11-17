package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Stores uploaded binaries (models, textures, CSV files) in GridFS.
 * The GridFS id doubles as the id of the matching {@link FileDocument}.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BinaryFileStore {

    private final GridFsOperations gridFs;

    /**
     * Streams an uploaded file into GridFS.
     *
     * @param file uploaded file.
     * @return the assigned GridFS id, in hex form.
     */
    public String store(MultipartFile file) {
        try {
            ObjectId objectId = gridFs.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
            return objectId.toHexString();
        } catch (IOException e) {
            throw new IllegalStateException("Soubor " + file.getOriginalFilename() + " se nepodařilo uložit.", e);
        }
    }

    /**
     * @param fileId GridFS id.
     * @return the stored binary, or {@code null} when nothing is stored under that id.
     */
    public GridFsResource find(String fileId) {
        if (!ObjectId.isValid(fileId)) {
            return null;
        }
        GridFSFile file = gridFs.findOne(byId(fileId));
        return file == null ? null : gridFs.getResource(file);
    }

    /**
     * @param fileId GridFS id.
     * @return whether a binary is stored under that id.
     */
    public boolean exists(String fileId) {
        if (!ObjectId.isValid(fileId)) {
            return false;
        }
        return gridFs.find(byId(fileId).limit(1)).first() != null;
    }

    /**
     * Removes a binary. Missing or malformed ids are ignored so cleanup stays idempotent.
     *
     * @param fileId GridFS id.
     */
    public void delete(String fileId) {
        if (!ObjectId.isValid(fileId)) {
            log.warn("Přeskakuji mazání souboru s neplatným ID: {}", fileId);
            return;
        }
        gridFs.delete(byId(fileId));
    }

    private static Query byId(String fileId) {
        return new Query(Criteria.where("_id").is(new ObjectId(fileId)));
    }
}
