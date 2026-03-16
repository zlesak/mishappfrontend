package cz.uhk.zlesak.threejslearningapp.domain.parsers;

import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelListingDataParserTest {

    @Test
    void modelForSelectDataParser_shouldDeduplicateModelsAndKeepMainFlag() {
        var mainTexture = TestFixtures.texture("main-texture", "model-1", "Main texture", null);
        var main = TestFixtures.model("main", "model-1", "Femur", mainTexture, List.of());
        var duplicate = TestFixtures.model("sub-1", "model-1", "Femur duplicate", mainTexture, List.of());
        var second = TestFixtures.model("sub-2", "model-2", "Humerus", null, List.of());

        Map<String, cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity> models = new LinkedHashMap<>();
        models.put("main", main);
        models.put("sub-1", duplicate);
        models.put("sub-2", second);

        List<ModelForSelect> parsed = ModelListingDataParser.modelForSelectDataParser(models);

        assertEquals(2, parsed.size());
        assertEquals("model-1", parsed.getFirst().id());
        assertTrue(parsed.getFirst().mainItem());
        assertEquals("main-texture", parsed.getFirst().mainTextureId());
        assertEquals("model-2", parsed.get(1).id());
        assertFalse(parsed.get(1).mainItem());
    }

    @Test
    void modelForSelectDataParser_shouldReturnEmptyListForNullOrEmptyInput() {
        assertTrue(ModelListingDataParser.modelForSelectDataParser(null).isEmpty());
        assertTrue(ModelListingDataParser.modelForSelectDataParser(Map.of()).isEmpty());
    }
}
