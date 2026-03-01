package cz.uhk.zlesak.threejslearningapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * InputFileDesc - DTO describing a file and its related files for the model upload API.
 * Sent as the {@code metadata} request part to {@code POST /api/model/upload}.
 * The structure is recursive: a model may have texture children, which may have CSV children.
 * Copies backend structure for proper fetch of file metadata without the need for multiple calls.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputFileDesc {
    String originalFileName;
    String name;
    String description;
    FileSenseType fileSenseType;
    List<InputFileDesc> relatedFiles;
    String id;
}
