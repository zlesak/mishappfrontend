package cz.uhk.zlesak.threejslearningapp.domain.texture;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * TextureFilter Class - Represents filter criteria for querying textures.
 * Not currently used in the application as textures are connected to models directly.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class TextureFilter extends FilterBase {
    String ModelId;
}
