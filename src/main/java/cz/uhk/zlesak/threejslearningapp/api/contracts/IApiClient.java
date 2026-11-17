package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;

/**
 * CRUD operations the UI performs on one kind of entity.
 * This is the boundary between the Vaadin layer and the backend: the UI never touches repositories
 * or documents directly.
 *
 * @param <E> full entity, used on detail screens
 * @param <Q> lightweight entity, used in listings
 * @param <F> filter type used for filtered listing
 */
public interface IApiClient<E, Q, F> {

    /**
     * Creates a new entity.
     *
     * @param entity Entity to create
     * @return Created entity
     * @throws Exception if creation fails
     */
    Q create(E entity) throws Exception;

    /**
     * Reads an entity by its ID.
     *
     * @param id ID of the entity to read
     * @return Read entity
     * @throws Exception if reading fails
     */
    E read(String id) throws Exception;

    /**
     * Reads a quick version of an entity by its ID.
     *
     * @param id ID of the entity to read
     * @return Quick version of the entity
     * @throws Exception if reading fails
     */
    Q readQuick(String id) throws Exception;

    /**
     * Reads entities in a paginated manner, filtered by the provided parameters.
     *
     * @param pageRequest paging and filtering to apply
     * @return PageResult of entities of type Q
     * @throws Exception if reading fails
     */
    PageResult<Q> readEntities(FilterParameters<F> pageRequest) throws Exception;

    /**
     * Updates an existing entity by its ID.
     *
     * @param id ID of the entity to update
     * @param entity Updated entity
     * @return Updated entity
     * @throws Exception if updating fails
     */
    E update(String id, E entity) throws Exception;

    /**
     * Deletes an entity by its ID.
     *
     * @param id ID of the entity to delete
     * @return True if deletion was successful, false otherwise
     * @throws Exception if deletion fails
     */
    boolean delete(String id) throws Exception;
}
