package cz.uhk.zlesak.threejslearningapp.domain.model;

import cz.uhk.zlesak.threejslearningapp.domain.file.QuickFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickModelEntity Class - Represents a lightweight model entity with associated textures and metadata.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickModelEntity extends QuickFileEntity {
    String metadataId;
    ModelFileEntity model;
    boolean isAdvanced;
    QuickTextureEntity mainTexture;
    List<QuickTextureEntity> otherTextures;
    public List<QuickTextureEntity> getAllTextures(){
        List<QuickTextureEntity> allTextures = new ArrayList<>();
        if(otherTextures != null && !otherTextures.isEmpty()){
            allTextures.addAll(otherTextures);
        }
        if(mainTexture != null){
            allTextures.addFirst(mainTexture);
        }
        return allTextures;
    }
}
