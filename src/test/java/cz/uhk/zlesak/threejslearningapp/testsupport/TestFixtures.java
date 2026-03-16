package cz.uhk.zlesak.threejslearningapp.testsupport;

import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TestFixtures {
    private TestFixtures() {
    }

    public static QuickTextureEntity texture(String id, String modelId, String name, String csvContent) {
        return QuickTextureEntity.builder()
                .id(id)
                .textureFileId(id)
                .modelId(modelId)
                .name(name)
                .csvContent(csvContent)
                .build();
    }

    public static QuickModelEntity model(
            String metadataId,
            String modelId,
            String name,
            QuickTextureEntity mainTexture,
            List<QuickTextureEntity> otherTextures
    ) {
        return QuickModelEntity.builder()
                .id(metadataId)
                .metadataId(metadataId)
                .name(name)
                .model(ModelFileEntity.builder().id(modelId).name(name).build())
                .mainTexture(mainTexture)
                .otherTextures(otherTextures)
                .build();
    }

    @SuppressWarnings("unused")
    public static Map<String, QuickModelEntity> chapterModels(QuickModelEntity... models) {
        Map<String, QuickModelEntity> result = new LinkedHashMap<>();
        for (int i = 0; i < models.length; i++) {
            result.put(i == 0 ? "main" : "sub-" + i, models[i]);
        }
        return result;
    }

    @SuppressWarnings("unused")
    public static DocumentationEntry documentationEntry(String id, String type, String title) {
        return new DocumentationEntry(id, type, title, "{}", List.of());
    }

    @SuppressWarnings("unused")
    public static QuizEntity quizEntity(String id, String name) {
        return QuizEntity.builder().id(id).name(name).description("desc").build();
    }
}
