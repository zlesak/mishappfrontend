package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;

/**
 * Common interface for all services.
 */
interface IService<E, Q, F> {

    /**
     * Creates a new entity.
     *
     * @param createEntity Entity to create.
     * @return Created entity.
     */
    String create(E createEntity) throws RuntimeException;

    /**
     * Reads an entity by its ID.
     *
     * @param id Entity ID.
     * @return Read entity.
     */
    E read(String id) throws RuntimeException;

    /**
     * Reads a quick entity by its ID.
     *
     * @param id Entity ID.
     * @return Read entity.
     */
    Q readQuick(String id) throws RuntimeException;

    /**
     * Reads entities with pagination.
     *
     * @return Page result of entities.
     */
    PageResult<Q> readEntities(FilterParameters<F> pageRequest) throws RuntimeException;

    /**
     * Updates an existing entity.
     *
     * @param id     Entity ID.
     * @param entity Entity to update.
     * @return Updated entity.
     */
    String update(String id, E entity) throws RuntimeException;

    /**
     * Deletes an entity by its ID.
     *
     * @param id Entity ID.
     * @return True if deletion was successful, false otherwise.
     */
    boolean delete(String id) throws RuntimeException;


}
