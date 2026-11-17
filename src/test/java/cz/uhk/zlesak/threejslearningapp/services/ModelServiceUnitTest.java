package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.contracts.IModelApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.*;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelServiceUnitTest {
    private IModelApiClient modelApiClient;
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        modelApiClient = mock(IModelApiClient.class);
        modelService = spy(new ModelService(modelApiClient));
    }

    @Test
    void parseModelDescriptionShouldHandleTextualBackgroundNodeWithInvalidJsonContent() {
        
        
        String description = "{\"thumbnailDataUrl\":\"thumb\",\"background\":\"{invalid-json\"}";
        ModelService.ModelDescriptionData result = modelService.parseModelDescription(description);
        assertEquals("thumb", result.thumbnailDataUrl());
        assertNull(result.backgroundSpecJson());
    }

    @Test
    void extensionForMimeTypeShouldReturnJpgForNullMimeType() throws Exception {
        
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extensionForMimeType", String.class);
        m.setAccessible(true);
        assertEquals("jpg", m.invoke(modelService, (Object) null));
    }

    @Test
    void extractModelDownloadFileIdShouldReturnNullForBlankUrl() throws Exception {
        
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extractModelDownloadFileId", String.class);
        m.setAccessible(true);
        assertNull(m.invoke(modelService, "   "));
        assertNull(m.invoke(modelService, (Object) null));
    }

    private InputStreamMultipartFile file(String name, String content) {
        return InputStreamMultipartFile.builder()
                .fileName(name)
                .displayName(name)
                .inputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @Test
    void extensionForMimeTypeShouldReturnPngForPngMimeType() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extensionForMimeType", String.class);
        m.setAccessible(true);
        assertEquals("png", m.invoke(modelService, "image/png"));
    }

    @Test
    void extensionForMimeTypeShouldReturnWebpForWebpMimeType() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extensionForMimeType", String.class);
        m.setAccessible(true);
        assertEquals("webp", m.invoke(modelService, "image/webp"));
    }

    @Test
    void extensionForMimeTypeShouldReturnGifForGifMimeType() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extensionForMimeType", String.class);
        m.setAccessible(true);
        assertEquals("gif", m.invoke(modelService, "image/gif"));
    }

    @Test
    void extensionForMimeTypeShouldReturnJpgForJpegMimeType() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extensionForMimeType", String.class);
        m.setAccessible(true);
        assertEquals("jpg", m.invoke(modelService, "image/jpeg"));
    }

    @Test
    void extensionForMimeTypeShouldReturnJpgForUnrecognizedMimeType() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extensionForMimeType", String.class);
        m.setAccessible(true);
        assertEquals("jpg", m.invoke(modelService, "application/octet-stream"));
    }

    @Test
    void extractModelDownloadFileIdShouldExtractIdFromValidUrl() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extractModelDownloadFileId", String.class);
        m.setAccessible(true);
        String url = "https://example.com/api/model/download/abc123";
        assertEquals("abc123", m.invoke(modelService, url));
    }

    @Test
    void extractModelDownloadFileIdShouldExtractIdFromUrlWithQueryString() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extractModelDownloadFileId", String.class);
        m.setAccessible(true);
        String url = "https://example.com/api/model/download/xyz789?token=abc";
        assertEquals("xyz789", m.invoke(modelService, url));
    }

    @Test
    void extractModelDownloadFileIdShouldReturnNullWhenMarkerNotPresent() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extractModelDownloadFileId", String.class);
        m.setAccessible(true);
        assertNull(m.invoke(modelService, "https://example.com/api/other/resource/id123"));
    }

    @Test
    void extractModelDownloadFileIdShouldReturnNullWhenIdPartIsEmpty() throws Exception {
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extractModelDownloadFileId", String.class);
        m.setAccessible(true);
        assertNull(m.invoke(modelService, "https://example.com/api/model/download/   "));
    }

    @Test
    void parseModelDescriptionShouldReturnEmptyForNull() {
        ModelService.ModelDescriptionData data = modelService.parseModelDescription(null);
        assertEquals("", data.thumbnailDataUrl());
        assertNull(data.backgroundSpecJson());
    }

    @Test
    void parseModelDescriptionShouldReturnEmptyForBlank() {
        ModelService.ModelDescriptionData data = modelService.parseModelDescription("   ");
        assertEquals("", data.thumbnailDataUrl());
        assertNull(data.backgroundSpecJson());
    }

    @Test
    void parseModelDescriptionShouldHandleLegacyPlainThumbnailString() {
        ModelService.ModelDescriptionData data = modelService.parseModelDescription("data:image/png;base64,abc");
        assertEquals("data:image/png;base64,abc", data.thumbnailDataUrl());
        assertNull(data.backgroundSpecJson());
    }

    @Test
    void parseModelDescriptionShouldParseJsonWithObjectBackground() {
        String json = "{\"thumbnailDataUrl\":\"thumb\",\"background\":{\"type\":\"color\",\"value\":\"#abcdef\"}}";
        ModelService.ModelDescriptionData data = modelService.parseModelDescription(json);
        assertEquals("thumb", data.thumbnailDataUrl());
        assertNotNull(data.backgroundSpecJson());
        assertTrue(data.backgroundSpecJson().contains("color"));
    }

    @Test
    void parseModelDescriptionShouldHandleTextualColorValueInBackground() {
        String json = "{\"thumbnailDataUrl\":\"thumb\",\"background\":\"#ff0000\"}";
        ModelService.ModelDescriptionData data = modelService.parseModelDescription(json);
        assertEquals("thumb", data.thumbnailDataUrl());
        assertNotNull(data.backgroundSpecJson());
        assertTrue(data.backgroundSpecJson().contains("#ff0000"));
    }

    @Test
    void parseModelDescriptionShouldHandleLegacyBackgroundSpecKey() {
        String json = "{\"thumbnailDataUrl\":\"t\",\"backgroundSpec\":{\"type\":\"color\",\"value\":\"#112233\"}}";
        ModelService.ModelDescriptionData data = modelService.parseModelDescription(json);
        assertEquals("t", data.thumbnailDataUrl());
        assertNotNull(data.backgroundSpecJson());
        assertTrue(data.backgroundSpecJson().contains("color"));
    }

    @Test
    void parseModelDescriptionShouldHandleLegacyBackgroundSpecJsonKey() {
        String json = "{\"thumbnailDataUrl\":\"t2\",\"backgroundSpecJson\":\"{\\\"type\\\":\\\"color\\\",\\\"value\\\":\\\"#223344\\\"}\"}";
        ModelService.ModelDescriptionData data = modelService.parseModelDescription(json);
        assertEquals("t2", data.thumbnailDataUrl());
        assertNotNull(data.backgroundSpecJson());
        assertTrue(data.backgroundSpecJson().contains("color"));
    }




    @Test
    void buildPrefillDataShouldReturnModelFileOnly() throws Exception {
        InputStreamMultipartFile modelFile = file("model.glb", "model-data");
        when(modelApiClient.downloadFile("m-id")).thenReturn(modelFile);

        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder().id("m-id").build())
                .build();

        ModelService.ModelPrefillData result = modelService.buildPrefillData(entity);

        assertNotNull(result.modelFile());
        assertNull(result.mainTexture());
        assertNull(result.otherTextures());
        assertNull(result.csvFiles());
    }

    @Test
    void buildPrefillDataShouldHandleOtherTextureWithoutCsvRelatedFile() throws Exception {
        InputStreamMultipartFile modelFile = file("model.glb", "model-data");
        InputStreamMultipartFile otherFile = file("other.png", "other-data");
        when(modelApiClient.downloadFile("m-id")).thenReturn(modelFile);
        when(modelApiClient.downloadFile("other-id")).thenReturn(otherFile);

        QuickTextureEntity otherTex = QuickTextureEntity.builder()
                .id("other-id").name("other.png").csvContent(null).build();
        ModelEntity entity = ModelEntity.builder()
                .model(ModelFileEntity.builder().id("m-id").build())
                .otherTextures(List.of(otherTex))
                .build();

        ModelService.ModelPrefillData result = modelService.buildPrefillData(entity);

        assertNotNull(result.modelFile());
        assertNotNull(result.otherTextures());
        assertEquals(1, result.otherTextures().size());
        assertNull(result.csvFiles());
    }
}
