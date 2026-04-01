package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.ModelApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.model.*;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ModelServiceTest {
    private ModelApiClient modelApiClient;
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        modelApiClient = mock(ModelApiClient.class);
        modelService = new ModelService(modelApiClient);
    }

    @Test
    void saveFromUpload_shouldCreateEntityAndTrimName() throws Exception {
        when(modelApiClient.create(any(ModelEntity.class)))
                .thenReturn(QuickModelEntity.builder().id("meta-1").build());

        String id = modelService.saveFromUpload(
                null,
                "  Femur  ",
                file("model.glb", "model"),
                file("main.png", "main"),
                List.of(file("other.png", "other")),
                List.of(file("other.csv", "ff0000;Bone")),
                "thumb-data",
                null
        );

        assertEquals("meta-1", id);

        ArgumentCaptor<ModelEntity> captor = ArgumentCaptor.forClass(ModelEntity.class);
        verify(modelApiClient).create(captor.capture());
        ModelEntity created = captor.getValue();

        assertEquals("Femur", created.getName());
        assertEquals("thumb-data", created.getDescription());
        assertNotNull(created.getInputStreamMultipartFile());
        assertNotNull(created.getFullMainTexture());
        assertEquals(1, created.getFullOtherTextures().size());
        assertEquals(1, created.getCsvFiles().size());
    }

    @Test
    void saveFromUpload_shouldUseUpdateWhenMetadataExists() throws Exception {
        when(modelApiClient.update(any(), any()))
                .thenReturn(ModelEntity.builder().id("updated-id").build());

        String id = modelService.saveFromUpload(
                "meta-1",
                "Atlas",
                file("model.glb", "model"),
                null,
                null,
                null,
                "thumb",
                null
        );

        assertEquals("updated-id", id);
    }

    @Test
    void read_shouldRejectBlankId() {
        assertThrows(RuntimeException.class, () -> modelService.read(""));
    }

    @Test
    void encodeAndParseDescription_shouldPreserveBackgroundJson() {
        String background = "{\"type\":\"color\",\"value\":\"#112233\"}";
        String encoded = modelService.encodeModelDescription("thumb", background);

        assertTrue(encoded.contains("thumbnailDataUrl"));
        assertEquals("thumb", modelService.extractThumbnailDataUrl(encoded));
        assertEquals(background, modelService.extractBackgroundSpecJson(encoded));
    }

    private static InputStreamMultipartFile file(String name, String content) {
        return InputStreamMultipartFile.builder()
                .fileName(name)
                .displayName(name)
                .inputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                .build();
    }
    @Test
    void encodeModelDescription_shouldReturnThumbnailWhenBackgroundJsonIsInvalidAndCannotBeParsed() {
        String result = modelService.encodeModelDescription("thumb", "{invalid-json");
        assertEquals("thumb", result);
    }

    @Test
    void resolveBackgroundSpecJson_shouldReturnNullWhenRelatedFilesContainNoBackgroundImageEntry() {
        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder()
                        .id("model-file")
                        .related(List.of(ModelFileEntity.builder()
                                .id("other-1").senseType(FileSenseType.MODEL).build()))
                        .build())
                .description("legacy-thumb")
                .build();
        assertNull(modelService.resolveBackgroundSpecJson(entity));
    }

    @Test
    void extractBackgroundType_shouldReturnEmptyStringForBlankInput() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extractBackgroundType", String.class);
        m.setAccessible(true);
        assertEquals("", m.invoke(modelService, "   "));
        assertEquals("", m.invoke(modelService, (Object) null));
    }

}