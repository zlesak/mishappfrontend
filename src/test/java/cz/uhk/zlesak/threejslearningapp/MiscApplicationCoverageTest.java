package cz.uhk.zlesak.threejslearningapp;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.AppShellSettings;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsDoingActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsFinishedActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsLoadingProgress;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MiscApplicationCoverageTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void applicationBeansAndPageConfigurationShouldBeCreated() {
        MishAppFrontendApplication app = new MishAppFrontendApplication();
        AppShellSettings settings = mock(AppShellSettings.class);

        assertNotNull(app.restClient());
        assertFalse(app.objectMapper().isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        app.configurePage(settings);

        verify(settings).addFavIcon("icon", "/icons/MISH_icon.ico", "256x256");
        verify(settings).addLink("shortcut icon", "/icons/MISH_icon.ico");
        verify(settings).addMetaTag("description", "MISH APP - moderní systém učení");
        verify(settings).setPageTitle("MISH APP");
    }

    @Test
    void chapterEntityShouldConvertModelsForBackendRecursively() {
        ModelFileEntity detail = ModelFileEntity.builder().id("csv-1").name("detail.csv").senseType(FileSenseType.CSV_FILE).build();
        ModelFileEntity modelFile = ModelFileEntity.builder()
                .id("model-1")
                .name("organ.glb")
                .senseType(FileSenseType.MODEL)
                .related(List.of(detail))
                .build();

        ChapterEntity entity = ChapterEntity.builder()
                .name("Chapter")
                .content("{}")
                .models(new ArrayList<>(List.of(TestFixtures.model("meta-1", "ignored", "Model 1", null, List.of()))))
                .build();
        entity.getModels().add(null);
        entity.getModels().getFirst().setModel(modelFile);

        var modelsForBackend = entity.getModelsForBackend();

        assertEquals(1, modelsForBackend.size());
        assertEquals("meta-1", modelsForBackend.getFirst().getMetadataId());
        assertEquals("model-1", modelsForBackend.getFirst().getModel().getId());
        assertEquals("csv-1", modelsForBackend.getFirst().getModel().getRelated().getFirst().getId());
        entity.setModels(null);
        assertTrue(entity.getModelsForBackend().isEmpty());
    }

    @Test
    void smallRecordsAndEventsShouldExposePayload() {
        TextureAreaForSelect textureArea = new TextureAreaForSelect("texture-1", "#AABBCC", "Area", "model-1");
        assertEquals("texture-1", textureArea.primary());
        assertEquals("#AABBCC", textureArea.secondary());
        assertFalse(textureArea.mainItem());

        RemoveFileEvent removeFileEvent = new RemoveFileEvent(UI.getCurrent(), "model-1", FileType.CSV, "entity-1", true, "question-1");
        assertEquals("model-1", removeFileEvent.getModelId());
        assertEquals(FileType.CSV, removeFileEvent.getFileType());
        assertEquals("entity-1", removeFileEvent.getEntityId());
        assertEquals("question-1", removeFileEvent.getQuestionId());

        var selectedModel = TestFixtures.model("meta-2", "model-2", "Selected", null, List.of());
        ModelSelectedFromDialogEvent selectedEvent = new ModelSelectedFromDialogEvent(UI.getCurrent(), true, selectedModel, "block-7");
        assertEquals(selectedModel, selectedEvent.getSelectedModel());
        assertEquals("block-7", selectedEvent.getBlockId());

        ThreeJsComponent source = new ThreeJsComponent();
        ThreeJsDoingActions doingActions = new ThreeJsDoingActions(source, "loading");
        ThreeJsFinishedActions finishedActions = new ThreeJsFinishedActions(source);
        ThreeJsLoadingProgress loadingProgress = new ThreeJsLoadingProgress(source, 75, "textures");
        assertEquals("loading", doingActions.getDescription());
        assertEquals(75, loadingProgress.getPercent());
        assertEquals("textures", loadingProgress.getDescription());
        assertEquals(source, finishedActions.getSource());
    }
}
