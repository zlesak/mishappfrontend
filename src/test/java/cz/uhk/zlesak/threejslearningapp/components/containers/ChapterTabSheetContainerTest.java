package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.NameTextField;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ChapterContentScroller;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ModelsSelectScroller;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ChapterTabSheetContainerTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorAndResetShouldInitializeContentTab() {
        TestEditorJs editorJs = new TestEditorJs(CompletableFuture.completedFuture(Map.of()));
        ChapterTabSheetContainer container = new ChapterTabSheetContainer(
                new NameTextField("chapter.name"),
                new ChapterContentScroller(editorJs),
                new RecordingModelsSelectScroller()
        );

        container.setSelectedIndex(1);
        container.setMainContentTabSelected();

        assertEquals(0, container.getSelectedIndex());
        assertTrue(container.getChildren().findAny().isPresent());
    }

    @Test
    void initShouldPopulateModelSelectsWhenModelsTabOpens() {
        TestEditorJs editorJs = new TestEditorJs(CompletableFuture.completedFuture(Map.of("sub-1", "Subchapter")));
        ChapterContentScroller chapterContentScroller = new ChapterContentScroller(editorJs);
        RecordingModelsSelectScroller modelsScroller = new RecordingModelsSelectScroller();
        ChapterTabSheetContainer container = new ChapterTabSheetContainer(
                new NameTextField("chapter.name"),
                chapterContentScroller,
                modelsScroller
        );
        UI.getCurrent().add(container);

        container.init(editorJs);
        container.setSelectedIndex(1);

        assertEquals(Map.of("sub-1", "Subchapter"), modelsScroller.initialized);
    }

    @Test
    void initShouldKeepChapterScrollerEnabledWhenSubchapterLookupFails() {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("broken"));
        TestEditorJs editorJs = new TestEditorJs(future);
        ChapterContentScroller chapterContentScroller = new ChapterContentScroller(editorJs);
        RecordingModelsSelectScroller modelsScroller = new RecordingModelsSelectScroller();
        ChapterTabSheetContainer container = new ChapterTabSheetContainer(
                new NameTextField("chapter.name"),
                chapterContentScroller,
                modelsScroller
        );
        UI.getCurrent().add(container);

        container.init(editorJs);
        container.setSelectedIndex(1);

        assertNull(modelsScroller.initialized);
        assertTrue(chapterContentScroller.isEnabled());
    }

    private static final class TestEditorJs extends EditorJs {
        private final CompletableFuture<Map<String, String>> future;

        private TestEditorJs(CompletableFuture<Map<String, String>> future) {
            super(true);
            this.future = future;
        }

        @Override
        public CompletableFuture<Map<String, String>> getSubchaptersNames() {
            return future;
        }
    }

    private static final class RecordingModelsSelectScroller extends ModelsSelectScroller {
        private Map<String, String> initialized;

        @Override
        public void initSelects(Map<String, String> subChapterForSelectRecords) {
            initialized = subChapterForSelectRecords;
            super.initSelects(subChapterForSelectRecords);
        }
    }
}
