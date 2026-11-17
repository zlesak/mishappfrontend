package cz.uhk.zlesak.threejslearningapp.api;

import cz.uhk.zlesak.threejslearningapp.api.contracts.IModelApiClient;
import cz.uhk.zlesak.threejslearningapp.backend.service.FileStorageService;
import cz.uhk.zlesak.threejslearningapp.backend.service.ModelBackendService;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 3D model access for the UI.
 */
@Component
@RequiredArgsConstructor
public class ModelApiClient implements IModelApiClient {

    private final ModelBackendService modelBackendService;
    private final FileStorageService fileStorageService;

    @Override
    public QuickModelEntity create(ModelEntity entity) {
        return modelBackendService.upload(
                partsOf(entity),
                ModelUploadAssembler.describe(entity),
                descriptionOf(entity),
                entity.isAdvanced());
    }

    @Override
    public ModelEntity read(String id) {
        return ModelEntity.of(modelBackendService.require(id));
    }

    @Override
    public QuickModelEntity readQuick(String id) {
        return modelBackendService.require(id);
    }

    @Override
    public PageResult<QuickModelEntity> readEntities(FilterParameters<ModelFilter> filterParameters) {
        return modelBackendService.list(filterParameters);
    }

    @Override
    public ModelEntity update(String id, ModelEntity entity) {
        return ModelEntity.of(modelBackendService.update(
                id,
                partsOf(entity),
                ModelUploadAssembler.describe(entity),
                descriptionOf(entity),
                entity.isAdvanced()));
    }

    @Override
    public boolean delete(String id) {
        modelBackendService.delete(id);
        return true;
    }

    @Override
    public InputStreamMultipartFile downloadFile(String fileId) throws IOException {
        GridFsResource resource = fileStorageService.requireResource(fileId);
        return new InputStreamMultipartFile(resource.getInputStream(), resource.getFilename(), resource.getFilename());
    }

    private List<MultipartFile> partsOf(ModelEntity entity) {
        return new ArrayList<>(ModelUploadAssembler.files(entity));
    }

    private String descriptionOf(ModelEntity entity) {
        return entity.getDescription() == null ? "" : entity.getDescription();
    }
}
