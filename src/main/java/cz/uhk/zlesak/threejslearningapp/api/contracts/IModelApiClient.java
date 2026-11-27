package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;

/**
 * 3D model data access.
 */
public interface IModelApiClient extends IApiClient<ModelEntity, QuickModelEntity, ModelFilter> {

    /**
     * Downloads one stored file.
     *
     * @param fileId id of the file.
     * @return the file contents.
     * @throws Exception if the file cannot be read.
     */
    InputStreamMultipartFile downloadFile(String fileId) throws Exception;

    /**
     * @param fileId id of a stored file.
     * @return the name the file was uploaded under, without reading its contents.
     * @throws Exception if the file cannot be found.
     */
    String fileName(String fileId) throws Exception;
}
