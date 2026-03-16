package cz.uhk.zlesak.threejslearningapp.domain.parsers;

import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureListingDataParserTest {

    @Test
    void textureListingForSelectDataParser_shouldReturnFallbackWhenOtherTexturesMissing() {
        var model = TestFixtures.model("main", "model-1", "Femur", null, List.of());

        List<TextureListingForSelect> parsed = TextureListingDataParser.textureListingForSelectDataParser(
                Map.of("main", model), false, "No texture");

        assertEquals(1, parsed.size());
        assertEquals("model-1", parsed.getFirst().textureId());
        assertEquals("model-1", parsed.getFirst().modelId());
        assertEquals("No texture", parsed.getFirst().textureName());
    }

    @Test
    void textureListingForSelectDataParser_shouldDeduplicateByModelAndReturnTexturesForAllMode() {
        var mainTexture = TestFixtures.texture("main-t", "model-1", "Main", null);
        var otherTexture = TestFixtures.texture("other-t", "model-1", "Other", null);
        var main = TestFixtures.model("main", "model-1", "Femur", mainTexture, List.of(otherTexture));
        var duplicate = TestFixtures.model("sub-1", "model-1", "Femur duplicate", mainTexture, List.of(otherTexture));

        Map<String, cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity> models = new LinkedHashMap<>();
        models.put("main", main);
        models.put("sub-1", duplicate);

        List<TextureListingForSelect> parsed = TextureListingDataParser.textureListingForSelectDataParser(
                models, true, "No texture");

        assertEquals(2, parsed.size());
        assertTrue(parsed.stream().anyMatch(texture -> "main-t".equals(texture.textureId())));
        assertTrue(parsed.stream().anyMatch(texture -> "other-t".equals(texture.textureId())));
    }
}
