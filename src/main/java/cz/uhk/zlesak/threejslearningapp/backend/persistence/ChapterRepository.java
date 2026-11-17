package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** CRUD access to chapters. */
public interface ChapterRepository extends MongoRepository<ChapterEntity, String> {

    /**
     * @param modelId id of a model.
     * @return chapters that use the model, used to keep chapters from pointing at deleted models.
     */
    List<ChapterEntity> findByModelIdsContaining(String modelId);
}
