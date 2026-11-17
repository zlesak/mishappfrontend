package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChapterEntityTest {

    @Test
    void keepsTheStoredModelReferencesApartFromTheResolvedModels() {
        ChapterEntity chapter = ChapterEntity.builder()
                .name("Kapitola")
                .content("{}")
                .modelIds(List.of("model-1", "model-2"))
                .build();

        assertEquals(List.of("model-1", "model-2"), chapter.getModelIds());
        assertNull(chapter.getModels(), "modely se doplňují až při načtení kapitoly");
    }

    @Test
    void carriesTheResolvedModelsWithTheirFileHierarchy() {
        QuickModelEntity model = QuickModelEntity.builder()
                .id("model-1")
                .name("Kost")
                .model(ModelFileEntity.builder()
                        .id("file-1")
                        .name("bone.glb")
                        .senseType(FileSenseType.MODEL)
                        .related(List.of())
                        .build())
                .build();

        ChapterEntity chapter = ChapterEntity.builder()
                .modelIds(List.of("model-1"))
                .models(List.of(model))
                .build();

        assertEquals("model-1", chapter.getModels().getFirst().getId());
        assertEquals("file-1", chapter.getModels().getFirst().getModel().getId());
    }
}
