package cz.uhk.zlesak.threejslearningapp.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateModelMetadata – metadata sent as {@code modelMetadata} multipart part to
 * {@code PUT /api/model/update}.  Matches backend {@code UpdateModelMetadata}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModelMetadata {
    String id;
    String description;
    @JsonProperty("isAdvanced")
    boolean isAdvanced;
}
