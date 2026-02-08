package cz.uhk.zlesak.threejslearningapp.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Model file entity data class - holds data about model files on FE side or when communicating with backend API endpoints.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ModelFileEntity {
    String id;
    String name;
}
