package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChapterEntityTest {

    @Test
    void getModelsForBackend_shouldHandleNullModelFile_returningNullFileIdWithName() {
        QuickModelEntity modelWithNullFile = QuickModelEntity.builder()
                .id("meta-null")
                .metadataId("meta-null")
                .model(null)
                .build();

        ChapterEntity entity = ChapterEntity.builder()
                .models(List.of(modelWithNullFile))
                .build();

        List<ChapterEntity.ModelIds> result = entity.getModelsForBackend();

        assertEquals(1, result.size());
        assertEquals("meta-null", result.getFirst().getMetadataId());
        assertNull(result.getFirst().getModel());
    }

    @Test
    void getModelsForBackend_shouldHandleModelFileWithEmptyRelatedList() {
        ModelFileEntity modelFile = ModelFileEntity.builder()
                .id("mf-1")
                .name("bone.glb")
                .senseType(FileSenseType.MODEL)
                .related(List.of())
                .build();

        QuickModelEntity model = QuickModelEntity.builder()
                .id("meta-1")
                .metadataId("meta-1")
                .model(modelFile)
                .build();

        ChapterEntity entity = ChapterEntity.builder()
                .models(List.of(model))
                .build();

        List<ChapterEntity.ModelIds> result = entity.getModelsForBackend();

        assertEquals(1, result.size());
        assertEquals("mf-1", result.getFirst().getModel().getId());
        assertTrue(result.getFirst().getModel().getRelated().isEmpty());
    }
}
