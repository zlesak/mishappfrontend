package cz.uhk.zlesak.threejslearningapp.domain.model;

import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickModelEntityTest {

    private static QuickTextureEntity tex(String id) {
        return QuickTextureEntity.builder().id(id).textureFileId(id).name("Texture " + id).build();
    }

    @Test
    void getAllTextures_shouldReturnEmptyList_whenBothNull() {
        QuickModelEntity entity = QuickModelEntity.builder()
                .mainTexture(null)
                .otherTextures(null)
                .build();

        assertTrue(entity.getAllTextures().isEmpty());
    }

    @Test
    void getAllTextures_shouldReturnOnlyMainTexture_whenOtherTexturesNull() {
        QuickTextureEntity main = tex("main");
        QuickModelEntity entity = QuickModelEntity.builder()
                .mainTexture(main)
                .otherTextures(null)
                .build();

        List<QuickTextureEntity> result = entity.getAllTextures();
        assertEquals(1, result.size());
        assertEquals("main", result.getFirst().getId());
    }

    @Test
    void getAllTextures_shouldReturnOnlyMainTexture_whenOtherTexturesEmpty() {
        QuickTextureEntity main = tex("main");
        QuickModelEntity entity = QuickModelEntity.builder()
                .mainTexture(main)
                .otherTextures(List.of())
                .build();

        List<QuickTextureEntity> result = entity.getAllTextures();
        assertEquals(1, result.size());
        assertEquals("main", result.getFirst().getId());
    }

    @Test
    void getAllTextures_shouldReturnOtherTexturesOnly_whenMainTextureNull() {
        List<QuickTextureEntity> others = List.of(tex("a"), tex("b"));
        QuickModelEntity entity = QuickModelEntity.builder()
                .mainTexture(null)
                .otherTextures(others)
                .build();

        List<QuickTextureEntity> result = entity.getAllTextures();
        assertEquals(2, result.size());
        assertEquals("a", result.get(0).getId());
        assertEquals("b", result.get(1).getId());
    }

    @Test
    void getAllTextures_shouldPrependMainTexture_whenBothPresent() {
        QuickTextureEntity main = tex("main");
        List<QuickTextureEntity> others = List.of(tex("other1"), tex("other2"));
        QuickModelEntity entity = QuickModelEntity.builder()
                .mainTexture(main)
                .otherTextures(others)
                .build();

        List<QuickTextureEntity> result = entity.getAllTextures();
        assertEquals(3, result.size());
        assertEquals("main", result.getFirst().getId());
        assertEquals("other1", result.get(1).getId());
        assertEquals("other2", result.get(2).getId());
    }
}

