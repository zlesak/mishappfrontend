package cz.uhk.zlesak.threejslearningapp.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Model file entity data class - holds data about model files on FE side or when communicating with backend API endpoints.
 * Maps to backend {@code FileIdWithName}.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelFileEntity {
    String id;
    String name;
    FileSenseType senseType;

    @JsonProperty("relatedFiles")
    @JsonAlias({"related", "relatedFiles"})
    List<ModelFileEntity> related;
}
