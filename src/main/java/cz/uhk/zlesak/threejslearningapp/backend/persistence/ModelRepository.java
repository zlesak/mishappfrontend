package cz.uhk.zlesak.threejslearningapp.backend.persistence;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

/** CRUD access to 3D models. */
public interface ModelRepository extends MongoRepository<QuickModelEntity, String> {
}
