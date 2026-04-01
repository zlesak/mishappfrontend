package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.ModelApiClient;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileEntityRecursive;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileEntityTree;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
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
    private ModelApiClient modelApiClient;
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        modelApiClient = mock(ModelApiClient.class);
        modelService = spy(new ModelService(modelApiClient));
    }

    @Test
    void readShouldExitForLoopWhenOtherTextureHasOnlyNonCsvRelatedFiles() throws Exception {
        // Texture with a related file that is NOT CSV_FILE → the for loop in buildQuickTexture
        // iterates but the if condition is false → loop exits at line 585 without break
        VaadinTestSupport.setCurrentUi();
        try {
            FileEntityRecursive nonCsvChild = FileEntityRecursive.builder()
                    .id("model-child").name("child.glb").senseType(FileSenseType.MODEL).relatedFiles(List.of()).build();
            FileEntityRecursive otherTexture = FileEntityRecursive.builder()
                    .id("tex-other").name("other.jpg").senseType(FileSenseType.OTHER_TEXTURE)
                    .relatedFiles(List.of(nonCsvChild)).build();
            FileEntityTree tree = FileEntityTree.builder()
                    .id("model-file").name("Model").creatorId("u1").description("desc")
                    .created(Instant.now()).updated(Instant.now()).isAdvanced(false)
                    .allRelatedFiles(List.of(otherTexture)).build();
            when(modelApiClient.readFileEntityTree("meta-noc")).thenReturn(tree);

            ModelEntity loaded = modelService.read("meta-noc");

            assertNotNull(loaded);
            assertNotNull(loaded.getOtherTextures());
            assertEquals(1, loaded.getOtherTextures().size());
            assertNull(loaded.getOtherTextures().getFirst().getCsvContent());
        } finally {
            VaadinTestSupport.clearCurrentUi();
        }
    }

    @Test
    void parseModelDescriptionShouldHandleTextualBackgroundNodeWithInvalidJsonContent() {
        // background value is a textual string starting with '{' but not valid JSON
        // → normalizeBackgroundNodeToJson catches the JsonParseException (lines 501-505)
        String description = "{\"thumbnailDataUrl\":\"thumb\",\"background\":\"{invalid-json\"}";
        ModelService.ModelDescriptionData result = modelService.parseModelDescription(description);
        assertEquals("thumb", result.thumbnailDataUrl());
        assertNull(result.backgroundSpecJson());
    }

    @Test
    void extensionForMimeTypeShouldReturnJpgForNullMimeType() throws Exception {
        // Line 420: extensionForMimeType(null) returns "jpg"
        java.lang.reflect.Method m = ModelService.class.getDeclaredMethod("extensionForMimeType", String.class);
        m.setAccessible(true);
        assertEquals("jpg", m.invoke(modelService, (Object) null));
    }

    @Test
    void extractModelDownloadFileIdShouldReturnNullForBlankUrl() throws Exception {
        // Line 432: extractModelDownloadFileId with blank/null input returns null
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
}
