package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract service providing common CRUD operations.
 * @param <E> Entity type extending AbstractEntity
 * @param <Q> Quick entity type
 * @param <F> Filter type
 */
@Slf4j
public abstract class AbstractService<E extends Q, Q extends AbstractEntity, F> implements IService< E, Q, F> {
    E entity;
    Q quickEntity;
    IApiClient<E, Q, F> apiClient;

    /**
     * Constructor for AbstractService.
     * @param apiClient API client to be used for CRUD operations
     */
    public AbstractService(IApiClient<E, Q, F> apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Validates the create entity.
     * @param createEntity Entity to validate
     * @return Validated entity
     * @throws RuntimeException if validation fails
     */
    protected abstract E validateCreateEntity(E createEntity) throws RuntimeException;

    /**
     * Creates the final entity from the create entity.
     * @param createEntity Entity to create
     * @return Final entity
     * @throws RuntimeException if creation fails
     */
    protected abstract E createFinalEntity(E createEntity) throws RuntimeException;

    /**
     * Creates a new entity.
     *
     * @param createEntity Entity to create.
     * @return Created entity.
     * @throws RuntimeException if an error occurs during the creation process.
     */
    @Override
    public Q create(E createEntity) throws RuntimeException {
        try {
            return apiClient.create(createFinalEntity(validateCreateEntity(createEntity)));
        } catch (Exception e) {
            log.error("Chyba při vytváření enity: {}", e.getMessage(), e);
            throw new RuntimeException("Chyba při vytváření entity: " + e.getMessage(), e);
        }
    }

    /**
     * Reads an entity by its ID.
     *
     * @param entityId Entity ID.
     * @return Read entity.
     * @throws RuntimeException if an error occurs during the read operation.
     */
    @Override
    public E read(String entityId) throws RuntimeException {
        try {
            if (entityId == null || entityId.isEmpty()) {
                throw new RuntimeException("ID entity nesmí být prázdné.");
            }

            if (entity == null || entity.getId() == null || !entity.getId().equals(entityId)) {
                entity = apiClient.read(entityId);
            }
            return entity;
        } catch (Exception e) {
            log.error("Chyba při získávání entity: {}", e.getMessage(), e);
            throw new RuntimeException("Chyba při získávání entity: " + e.getMessage());
        }
    }
    /**
     * Reads a quick version of entity by its ID.
     *
     * @param entityId Entity ID.
     * @return Read entity.
     * @throws RuntimeException if an error occurs during the read operation.
     */
    @Override
    public Q readQuick(String entityId) throws RuntimeException {
        try {
            if (entityId == null || entityId.isEmpty()) {
                throw new RuntimeException("ID entity nesmí být prázdné.");
            }

            if (quickEntity == null || quickEntity.getId() == null || quickEntity.getId().isEmpty()) {
                quickEntity = apiClient.readQuick(entityId);
            }
            return quickEntity;
        } catch (Exception e) {
            log.error("Chyba při získávání quick entity: {}", e.getMessage(), e);
            throw new RuntimeException("Chyba při získávání quick entity: " + e.getMessage());
        }
    }

    /**
     * Reads entities with pagination.
     *
     * @param filterParameters Filter parameters for pagination.
     * @return Page result of entities.
     * @throws RuntimeException if an error occurs during the read operation.
     */
    @Override
    public PageResult<Q> readEntities(FilterParameters<F> filterParameters) throws RuntimeException {
        try {
            return apiClient.readEntities(filterParameters);
        } catch (Exception e) {
            log.error("Chyba při získávání stránkování entit pro page {}, limit {}, error message: {}", filterParameters.getPageRequest().getPageNumber(), filterParameters.getPageRequest().getPageSize(), e.getMessage(), e);
            throw new RuntimeException("Chyba při získávání entit: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing entity.
     *
     * @param id     Entity ID.
     * @param entity Entity to update.
     * @return Updated entity.
     * @throws RuntimeException if an error occurs during the update operation.
     */
    @Override
    public E update(String id, E entity) throws RuntimeException {
        try {
            if (id == null || id.isEmpty()) {
                throw new RuntimeException("ID entity nesmí být prázdné.");
            }
            validateCreateEntity(entity);
        } catch (RuntimeException e) {
            log.error("Chyba při validaci entity před aktualizací: {}", e.getMessage(), e);
            throw new RuntimeException("Chyba při validaci entity před aktualizací: " + e.getMessage(), e);
        }
        try {
            return apiClient.update(id, createFinalEntity(entity));
        } catch (Exception e) {
            log.error("Chyba při aktualizaci kapitoly: {}", e.getMessage(), e);
            throw new RuntimeException("Chyba při aktualizaci kapitoly: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id Entity ID.
     * @return True if deletion was successful, false otherwise.
     * @throws RuntimeException if an error occurs during the deletion process.
     */
    @Override
    public boolean delete(String id) throws RuntimeException {
        try {
            if (id == null || id.isEmpty()) {
                throw new RuntimeException("ID entity nesmí být prázdné.");
            }
            return apiClient.delete(id);
        } catch (Exception e) {
            log.error("Chyba při mazání entity: {}", e.getMessage(), e);
            throw new RuntimeException("Chyba při mazání entity: " + e.getMessage(), e);
        }
    }
}
