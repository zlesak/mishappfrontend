package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;

/**
 * Interface for generic API Client
 * Defines CRUD operations and listing for entities of type T and returns entities of type S in paginated results.
 *
 * @param <E> Entity type that this API client works with
 * @param <Q> Entity type that is returned in paginated results
 * @param <F> Filter type used for filtered listing
 */
public interface IApiClient <E, Q, F> {
    static String getBaseUrl() {
        boolean isHotswap = java.lang.management.ManagementFactory.getRuntimeMXBean()
                .getInputArguments().stream()
                .anyMatch(arg -> arg.contains("hotswap-agent.jar"));
        if (isHotswap) {
            return "http://localhost:8080/api/";
        }
        return "http://kotlin-backend:8080/api/";
    }

    static  String getLocalBaseBeUrl() {
        return "http://localhost:8080/api/";
    }

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
     * @param id ID of the entity to read
     * @return Quick version of the entity
     * @throws Exception if reading fails
     */
    Q readQuick(String id) throws Exception;

    /**
     * Reads entities in a paginated manner.
     * Filtered by the provided FilterParameters.
     *
     * @param pageRequest PageRequest object containing pagination info
     * @return PageResult of entities of type S
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

