package cz.uhk.zlesak.threejslearningapp.components.editors;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ScrollToElement;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ShowSubchapterContentEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
class EditorJsTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldSetReadOnlyPropertyAndRegisterTextureClickListener() {
        EditorJs editor = new EditorJs(false);

        assertTrue(editor.getElement().getProperty("readOnly", false));

        ElementListenerMap listenerMap = editor.getElement().getNode().getFeature(ElementListenerMap.class);
        assertTrue(listenerMap.getExpressions("texturecolorareaclick").contains("event.detail.modelId"));
        assertTrue(listenerMap.getExpressions("texturecolorareaclick").contains("event.detail.textureId"));
        assertTrue(listenerMap.getExpressions("texturecolorareaclick").contains("event.detail.hexColor"));
    }

    @Test
    void getDataShouldResolveJsonAndFallbackToEmptyObject() {
        EditorJs editor = attach(new EditorJs(true));

        var explicitData = editor.getData();
        drainInvocation("getData").complete(JsonNodeFactory.instance.textNode("{\"blocks\":[]}"));
        assertEquals("{\"blocks\":[]}", explicitData.join());

        var emptyData = editor.getData();
        drainInvocation("getData").complete(JsonNodeFactory.instance.textNode(""));
        assertEquals("{}", emptyData.join());
    }

    @Test
    void jsCommandMethodsShouldQueueExpectedInvocations() {
        EditorJs editor = attach(new EditorJs(true));

        editor.setChapterContentData("{\"blocks\":[]}");
        assertTrue(drainInvocation("setChapterContentData").getInvocation().getExpression().contains("setChapterContentData"));

        editor.filterContentByLevel1Header("sub-1");
        assertTrue(drainInvocation("filterContentByLevel1Header").getInvocation().getExpression().contains("filterContentByLevel1Header"));

        editor.scrollToHeading("sub-2");
        assertTrue(drainInvocation("scrollToDataId").getInvocation().getExpression().contains("scrollToDataId"));

        editor.search("atlas");
        assertTrue(drainInvocation("search").getInvocation().getExpression().contains("search"));

        editor.loadMoodleHtml("<p>hello</p>");
        assertTrue(drainInvocation("loadMoodleHtml").getInvocation().getExpression().contains("loadMoodleHtml"));
    }

    @Test
    void initializeTextureSelectsShouldQueueSerializedData() {
        EditorJs editor = attach(new EditorJs(true));

        editor.initializeTextureSelects(Map.of(
                "main", TestFixtures.model("meta-main", "model-main", "Main", null, List.of())
        ));

        PendingJavaScriptInvocation invocation = drainInvocation("initializeModelTextureAreaSelects");
        assertTrue(invocation.getInvocation().getExpression().contains("initializeModelTextureAreaSelects"));
        assertEquals(4, invocation.getInvocation().getParameters().size());
    }

    @Test
    void getSubchaptersNamesShouldParseJsonAndWrapInvalidPayload() {
        EditorJs editor = attach(new EditorJs(true));

        var result = editor.getSubchaptersNames();
        drainInvocation("getSubchaptersNames").complete(JsonNodeFactory.instance.textNode("{\"sub-1\":\"Intro\"}"));
        assertEquals(Map.of("sub-1", "Intro"), result.join());

        var invalid = editor.getSubchaptersNames();
        drainInvocation("getSubchaptersNames").complete(JsonNodeFactory.instance.textNode("not-json"));
        assertThrows(CompletionException.class, invalid::join);
    }

    @Test
    void attachEventsShouldTriggerFilterAndScrollInvocationsAndDetachShouldClearRegistrations() {
        EditorJs editor = attach(new EditorJs(true));

        ComponentUtil.fireEvent(UI.getCurrent(), new ShowSubchapterContentEvent(UI.getCurrent(), "sub-1", "heading-1", true));
        assertTrue(drainInvocation("filterContentByLevel1Header").getInvocation().getExpression().contains("filterContentByLevel1Header"));

        ComponentUtil.fireEvent(UI.getCurrent(), new ScrollToElement(UI.getCurrent(), "sub-2", true));
        assertTrue(drainInvocation("scrollToDataId").getInvocation().getExpression().contains("scrollToDataId"));

        editor.getElement().removeFromParent();
        assertEquals(0, getRegistrations(editor).size());
    }

    @Test
    void textureColorAreaClickShouldFireThreeJsActionEvent() {
        EditorJs editor = attach(new EditorJs(true));
        AtomicReference<ThreeJsActionEvent> captured = new AtomicReference<>();
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, captured::set);

        ObjectNode eventData = JsonNodeFactory.instance.objectNode();
        eventData.put("event.detail.modelId", "model-1");
        eventData.put("event.detail.textureId", "texture-1");
        eventData.put("event.detail.hexColor", "#AA11CC");

        ElementListenerMap listenerMap = editor.getElement().getNode().getFeature(ElementListenerMap.class);
        listenerMap.fireEvent(new DomEvent(editor.getElement(), "texturecolorareaclick", eventData));

        assertNotNull(captured.get());
        assertEquals("model-1", captured.get().getModelId());
        assertEquals("texture-1", captured.get().getTextureId());
        assertEquals("#AA11CC", captured.get().getMaskColor());
    }

    private EditorJs attach(EditorJs editor) {
        UI.getCurrent().add(editor);
        return editor;
    }

    private PendingJavaScriptInvocation drainInvocation(String expressionFragment) {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> invocations = UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations();
        assertFalse(invocations.isEmpty());
        return invocations.stream()
                .filter(invocation -> invocation.getInvocation().getExpression().contains(expressionFragment))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new NoSuchElementException("Missing invocation containing: " + expressionFragment));
    }

    private List<?> getRegistrations(EditorJs editor) {
        try {
            var field = EditorJs.class.getDeclaredField("registrations");
            field.setAccessible(true);
            return (List<?>) field.get(editor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

