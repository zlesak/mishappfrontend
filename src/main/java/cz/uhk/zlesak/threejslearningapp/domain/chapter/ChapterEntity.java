package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChapterEntity Class - Represents a detailed chapter entity with models, quizzes, and content.
 * @see QuickChapterEntity for the base class.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChapterEntity extends QuickChapterEntity {
    /** FE-only **/
    @JsonIgnore
    Map<String, QuickModelEntity> modelHeaderMap;
    String content;
    List<QuickModelEntity> models;
    /** FE-only **/
    @JsonIgnore
    List<QuickQuizEntity> quizzes;

    // Serialization for backend
    /**
     * When serializing ChapterEntity to send to backend, produce `models` in the shape
     * List<ModelIds> where each contains metadataId and a FileIdWithName structure.
     */
    @JsonGetter("models")
    public List<ModelIds> getModelsForBackend() {
        if (models == null) return List.of();
        List<ModelIds> out = new ArrayList<>();
        for (QuickModelEntity qm : models) {
            if (qm == null) continue;
            FileIdWithName file = convertModelFileEntityToFileIdWithName(qm.getModel());
            out.add(new ModelIds(qm.getMetadataId(), file));
        }
        return out;
    }

    private FileIdWithName convertModelFileEntityToFileIdWithName(ModelFileEntity mfe) {
        if (mfe == null) return null;
        FileIdWithName f = new FileIdWithName();
        f.setId(mfe.getId());
        f.setName(mfe.getName());
        f.setSenseType(mfe.getSenseType());
        List<FileIdWithName> rel = new ArrayList<>();
        if (mfe.getRelated() != null && !mfe.getRelated().isEmpty()) {
            for (ModelFileEntity child : mfe.getRelated()) {
                FileIdWithName childConv = convertModelFileEntityToFileIdWithName(child);
                if (childConv != null) rel.add(childConv);
            }
        }
        f.setRelated(rel);
        return f;
    }

    /** Pair of a model metadata ID and its primary file descriptor. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelIds {
        String metadataId;
        FileIdWithName model;
    }

    /** Minimal file descriptor carrying ID, name, sense-type, and optional related files. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileIdWithName {
        String id;
        String name;
        cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType senseType;
        List<FileIdWithName> related;
    }
}