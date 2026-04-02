package cz.uhk.zlesak.threejslearningapp.domain.texture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextureListingForSelectTest {

    @Test
    void mainItem_shouldReturnTrueWhenMainIsTrue() {
        TextureListingForSelect record = new TextureListingForSelect("tex-1", "model-1", "Texture Name", true);

        assertTrue(record.mainItem());
    }

    @Test
    void mainItem_shouldReturnFalseWhenMainIsFalse() {
        TextureListingForSelect record = new TextureListingForSelect("tex-2", "model-2", "Other Texture", false);

        assertFalse(record.mainItem());
    }

    @Test
    void mainItem_shouldReturnFalseWhenVarargsIsEmpty() {
        TextureListingForSelect record = new TextureListingForSelect("tex-3", "model-3", "No Main");

        assertFalse(record.mainItem());
    }

    @Test
    void primary_shouldReturnModelId() {
        TextureListingForSelect record = new TextureListingForSelect("tex-4", "model-4", "Some Texture");

        assertEquals("model-4", record.primary());
    }

    @Test
    void secondary_shouldReturnTextureId() {
        TextureListingForSelect record = new TextureListingForSelect("tex-5", "model-5", "Some Texture");

        assertEquals("tex-5", record.secondary());
    }

    @Test
    void recordFields_shouldBeCorrectlyAssigned() {
        TextureListingForSelect record = new TextureListingForSelect("tex-6", "model-6", "Test Name", true);

        assertEquals("tex-6", record.textureId());
        assertEquals("model-6", record.modelId());
        assertEquals("Test Name", record.textureName());
        assertArrayEquals(new boolean[]{true}, record.main());
    }
}

