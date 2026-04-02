package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.ModelApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.*;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    void read_shouldReturnEntityForValidId() throws Exception {
        FileEntityTree tree = FileEntityTree.builder()
                .id("model-1").name("Femur").creatorId("u1").description("thumb")
                .created(Instant.now()).updated(Instant.now())
                .isAdvanced(false).allRelatedFiles(List.of()).build();
        when(modelApiClient.readFileEntityTree("model-1")).thenReturn(tree);

        ModelEntity result = modelService.read("model-1");

        assertNotNull(result);
        assertEquals("model-1", result.getId());
        assertEquals("Femur", result.getName());
    }

    @Test
    void read_shouldCacheEntityOnSecondCall() throws Exception {
        FileEntityTree tree = FileEntityTree.builder()
                .id("model-2").name("Atlas").creatorId("u1").description("thumb")
                .created(Instant.now()).updated(Instant.now())
                .isAdvanced(false).allRelatedFiles(List.of()).build();
        when(modelApiClient.readFileEntityTree("model-2")).thenReturn(tree);

        modelService.read("model-2");
        modelService.read("model-2");

        verify(modelApiClient, times(1)).readFileEntityTree("model-2");
    }

    @Test
    void read_shouldThrowForNullId() {
        assertThrows(RuntimeException.class, () -> modelService.read(null));
    }

    @Test
    void encodeModelDescription_shouldReturnThumbnailWhenBackgroundIsNull() {
        String result = modelService.encodeModelDescription("thumb", null);
        assertEquals("thumb", result);
    }

    @Test
    void encodeModelDescription_shouldReturnThumbnailWhenBackgroundIsBlank() {
        String result = modelService.encodeModelDescription("thumb", "   ");
        assertEquals("thumb", result);
    }

    @Test
    void encodeModelDescription_shouldReturnJsonWithBothFieldsWhenBackgroundIsValid() {
        String bg = "{\"type\":\"color\",\"value\":\"#ff0000\"}";
        String result = modelService.encodeModelDescription("thumb", bg);
        assertTrue(result.contains("thumbnailDataUrl"));
        assertTrue(result.contains("thumb"));
        assertTrue(result.contains("color"));
    }

    @Test
    void encodeModelDescription_shouldReturnThumbnailWhenEncodedExceedsMaxLength() {
        String longThumbnail = "data:image/png;base64," + "A".repeat(260_000);
        String bg = "{\"type\":\"color\",\"value\":\"#ff0000\"}";
        String result = modelService.encodeModelDescription(longThumbnail, bg);
        assertEquals(longThumbnail, result);
    }

    @Test
    void parseModelDescription_shouldReturnEmptyForNull() {
        ModelService.ModelDescriptionData data = modelService.parseModelDescription(null);
        assertEquals("", data.thumbnailDataUrl());
        assertNull(data.backgroundSpecJson());
    }

    @Test
    void parseModelDescription_shouldReturnEmptyForBlank() {
        ModelService.ModelDescriptionData data = modelService.parseModelDescription("   ");
        assertEquals("", data.thumbnailDataUrl());
        assertNull(data.backgroundSpecJson());
    }

    @Test
    void parseModelDescription_shouldHandleLegacyPlainThumbnailString() {
        ModelService.ModelDescriptionData data = modelService.parseModelDescription("data:image/png;base64,abc");
        assertEquals("data:image/png;base64,abc", data.thumbnailDataUrl());
        assertNull(data.backgroundSpecJson());
    }

    @Test
    void parseModelDescription_shouldParseValidJsonWithBackgroundObject() {
        String json = "{\"thumbnailDataUrl\":\"thumb\",\"background\":{\"type\":\"color\",\"value\":\"#123456\"}}";
        ModelService.ModelDescriptionData data = modelService.parseModelDescription(json);
        assertEquals("thumb", data.thumbnailDataUrl());
        assertNotNull(data.backgroundSpecJson());
        assertTrue(data.backgroundSpecJson().contains("color"));
    }

    @Test
    void extractThumbnailDataUrl_shouldReturnThumbnailFromEncodedDescription() {
        String bg = "{\"type\":\"color\",\"value\":\"#aabbcc\"}";
        String encoded = modelService.encodeModelDescription("my-thumb", bg);
        assertEquals("my-thumb", modelService.extractThumbnailDataUrl(encoded));
    }

    @Test
    void extractThumbnailDataUrl_shouldReturnPlainStringForLegacyDescription() {
        assertEquals("legacy-thumb", modelService.extractThumbnailDataUrl("legacy-thumb"));
    }

    @Test
    void extractBackgroundSpecJson_shouldReturnBackgroundFromEncodedDescription() {
        String bg = "{\"type\":\"color\",\"value\":\"#001122\"}";
        String encoded = modelService.encodeModelDescription("thumb", bg);
        String result = modelService.extractBackgroundSpecJson(encoded);
        assertNotNull(result);
        assertTrue(result.contains("color"));
    }

    @Test
    void extractBackgroundSpecJson_shouldReturnNullForLegacyDescription() {
        assertNull(modelService.extractBackgroundSpecJson("plain-thumb-string"));
    }

    @Test
    void extractBackgroundSpecJson_shouldReturnNullForNullDescription() {
        assertNull(modelService.extractBackgroundSpecJson(null));
    }

    @Test
    void resolveBackgroundSpecJson_shouldReturnNullForNullEntity() {
        assertNull(modelService.resolveBackgroundSpecJson(null));
    }

    @Test
    void resolveBackgroundSpecJson_shouldBuildImageJsonWhenBackgroundImageFileIsPresent() {
        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder()
                        .id("model-file")
                        .related(List.of(ModelFileEntity.builder()
                                .id("bg-file-1").senseType(FileSenseType.BACKGROUND_IMAGE).build()))
                        .build())
                .build();
        String result = modelService.resolveBackgroundSpecJson(entity);
        assertNotNull(result);
        assertTrue(result.contains("\"type\":\"image\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    void resolveBackgroundSpecJson_shouldFallbackToDescriptionWhenNoBackgroundImageFile() {
        String bg = "{\"type\":\"color\",\"value\":\"#ffffff\"}";
        String encoded = modelService.encodeModelDescription("thumb", bg);
        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder()
                        .id("model-file")
                        .related(List.of())
                        .build())
                .description(encoded)
                .build();
        String result = modelService.resolveBackgroundSpecJson(entity);
        assertNotNull(result);
        assertTrue(result.contains("color"));
    }

    @Test
    void resolveBackgroundSpecJson_shouldReturnNullWhenNoBackgroundAndEmptyDescription() {
        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder()
                        .id("model-file")
                        .related(List.of())
                        .build())
                .description(null)
                .build();
        assertNull(modelService.resolveBackgroundSpecJson(entity));
    }

    @Test
    void downloadFile_shouldDelegateToModelApiClient() throws Exception {
        InputStreamMultipartFile expected = file("model.glb", "binary-content");
        when(modelApiClient.downloadFile("file-id-1")).thenReturn(expected);

        InputStreamMultipartFile result = modelService.downloadFile("file-id-1");

        assertSame(expected, result);
        verify(modelApiClient).downloadFile("file-id-1");
    }

    @Test
    void buildPrefillData_shouldDownloadModelAndMainTexture() throws Exception {
        InputStreamMultipartFile modelFile = file("model.glb", "model-data");
        InputStreamMultipartFile textureFile = file("main.png", "texture-data");
        when(modelApiClient.downloadFile("model-id")).thenReturn(modelFile);
        when(modelApiClient.downloadFile("texture-id")).thenReturn(textureFile);

        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder().id("model-id").build())
                .mainTexture(QuickTextureEntity.builder()
                        .id("texture-id").name("main.png").build())
                .build();

        ModelService.ModelPrefillData result = modelService.buildPrefillData(entity);

        assertNotNull(result.modelFile());
        assertNotNull(result.mainTexture());
        assertNull(result.otherTextures());
        assertNull(result.csvFiles());
    }

    @Test
    void buildPrefillData_shouldHandleNullMainTexture() throws Exception {
        InputStreamMultipartFile modelFile = file("model.glb", "model-data");
        when(modelApiClient.downloadFile("model-id")).thenReturn(modelFile);

        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder().id("model-id").build())
                .build();

        ModelService.ModelPrefillData result = modelService.buildPrefillData(entity);

        assertNotNull(result.modelFile());
        assertNull(result.mainTexture());
        assertNull(result.otherTextures());
        assertNull(result.csvFiles());
    }

    @Test
    void buildPrefillData_shouldHandleNullOtherTextures() throws Exception {
        InputStreamMultipartFile modelFile = file("model.glb", "model-data");
        when(modelApiClient.downloadFile("model-id")).thenReturn(modelFile);

        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder().id("model-id").build())
                .otherTextures(null)
                .build();

        ModelService.ModelPrefillData result = modelService.buildPrefillData(entity);

        assertNotNull(result.modelFile());
        assertNull(result.otherTextures());
        assertNull(result.csvFiles());
    }

    @Test
    void saveFromUpload_shouldHandleNullCsvFiles() throws Exception {
        when(modelApiClient.create(any(ModelEntity.class)))
                .thenReturn(QuickModelEntity.builder().id("id-no-csv").build());

        String id = modelService.saveFromUpload(
                null, "Pelvis",
                file("model.glb", "data"),
                null, null, null,
                "thumb", null
        );

        assertEquals("id-no-csv", id);
        ArgumentCaptor<ModelEntity> captor = ArgumentCaptor.forClass(ModelEntity.class);
        verify(modelApiClient).create(captor.capture());
        assertNull(captor.getValue().getCsvFiles());
    }

    @Test
    void saveFromUpload_shouldHandleEmptyOtherTextures() throws Exception {
        when(modelApiClient.create(any(ModelEntity.class)))
                .thenReturn(QuickModelEntity.builder().id("id-empty-other").build());

        String id = modelService.saveFromUpload(
                null, "Scapula",
                file("model.glb", "data"),
                null, List.of(), null,
                "thumb", null
        );

        assertEquals("id-empty-other", id);
        ArgumentCaptor<ModelEntity> captor = ArgumentCaptor.forClass(ModelEntity.class);
        verify(modelApiClient).create(captor.capture());
        assertNull(captor.getValue().getFullOtherTextures());
    }

    @Test
    void saveFromUpload_shouldTrimNameForUpdatePath() throws Exception {
        when(modelApiClient.update(any(), any()))
                .thenReturn(ModelEntity.builder().id("updated-trimmed").build());

        modelService.saveFromUpload(
                "existing-meta", "  Tibia  ",
                file("model.glb", "data"),
                null, null, null,
                "thumb", null
        );

        ArgumentCaptor<ModelEntity> entityCaptor = ArgumentCaptor.forClass(ModelEntity.class);
        verify(modelApiClient).update(any(), entityCaptor.capture());
        assertEquals("Tibia", entityCaptor.getValue().getName());
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