package cz.uhk.zlesak.threejslearningapp.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

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
