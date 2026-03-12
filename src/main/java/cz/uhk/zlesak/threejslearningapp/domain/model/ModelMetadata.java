package cz.uhk.zlesak.threejslearningapp.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModelMetadata – metadata sent as {@code modelMetadata} multipart part to
 * {@code POST /api/model/upload}.
 * Matches backend {@code ModelMetadata}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetadata {
    String description;
    @JsonProperty("isAdvanced")
    boolean isAdvanced;
}
