package cz.uhk.zlesak.threejslearningapp.common;

import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextureMapHelperTest {

    @Test
    void createTextureAreaForSelectRecordList_shouldParseCsvRowsForUniqueModels() {
        String csv = "#ff0000;Head\n#00ff00;Neck";
        var texture1 = TestFixtures.texture("texture-1", "model-1", "Mask 1", csv);
        var texture2 = TestFixtures.texture("texture-2", "model-1", "Mask 2", "#0000ff;Body");
        var model = TestFixtures.model("main", "model-1", "Femur", null, List.of(texture1, texture2));
        var duplicate = TestFixtures.model("sub-1", "model-1", "Femur duplicate", null, List.of(texture1));

        Map<String, cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity> models = new LinkedHashMap<>();
        models.put("main", model);
        models.put("sub-1", duplicate);

        List<TextureAreaForSelect> records = TextureMapHelper.createTextureAreaForSelectRecordList(models);

        assertEquals(3, records.size());
        assertEquals("texture-1", records.getFirst().textureId());
        assertEquals("#ff0000", records.getFirst().hexColor());
        assertEquals("Head", records.getFirst().areaName());
    }

    @Test
    void csvParse_shouldThrowForInvalidRowFormat() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TextureMapHelper.csvParse("model-1", "invalid-row", new ArrayList<>(), "texture-1")
        );

        assertEquals("Invalid CSV format for TextureAreaForComboBoxRecord: invalid-row", ex.getMessage());
    }
}

