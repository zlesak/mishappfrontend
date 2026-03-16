package cz.uhk.zlesak.threejslearningapp.views.documentation;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.components.editors.DocumentationEntryEditor;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.services.DocumentationService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findButtonByText;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
@SuppressWarnings("DataFlowIssue")
class DocumentationViewKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private DocumentationService documentationService;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("student", "n/a", "ROLE_STUDENT"));
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void emptyDocumentationShouldRenderEmptyState() {
        when(documentationService.getEntries()).thenReturn(List.of());

        DocumentationView view = new DocumentationView(documentationService);
        UI.getCurrent().add(view);

        List<Span> spans = findAll(view, Span.class);
        assertTrue(spans.stream().anyMatch(span -> "Žádné položky k zobrazení.".equals(span.getText())));
    }

    @Test
    void filterButtonsShouldReloadEntriesByType() {
        DocumentationEntry allEntry = new DocumentationEntry("all-1", "chapter", "Úvod", "{}", List.of());
        DocumentationEntry chapterEntry = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());

        when(documentationService.getEntries()).thenReturn(List.of(allEntry));
        when(documentationService.getEntriesByType("chapter")).thenReturn(List.of(chapterEntry));

        DocumentationView view = new DocumentationView(documentationService);
        UI.getCurrent().add(view);

        assertTrue(findAll(view, Button.class).stream().anyMatch(button -> "--- Zobrazit vše ---".equals(button.getText())));
        assertTrue(findAll(view, Button.class).stream().anyMatch(button -> "Úvod".equals(button.getText())));

        findButtonByText(view, "Kapitoly").click();

        verify(documentationService).getEntriesByType("chapter");
        List<Button> buttons = findAll(view, Button.class);
        assertEquals(1, buttons.stream().filter(button -> "Kapitola 1".equals(button.getText())).count());
    }

    @Test
    void adminModeShouldEnterEditAddEntryAndSaveMergedEntries() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        DocumentationEntry chapterEntry = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry modelEntry = new DocumentationEntry("model-1", "model", "Model 1", "{}", List.of());
        when(documentationService.getEntries()).thenReturn(List.of(chapterEntry, modelEntry));
        when(documentationService.getEntriesByType("chapter")).thenReturn(List.of(chapterEntry));

        DocumentationView view = new DocumentationView(documentationService);
        UI.getCurrent().add(view);

        ReflectionTestUtils.invokeMethod(view, "navigateToType", "chapter");
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        Button editButton = (Button) ReflectionTestUtils.getField(view, "editButton");
        Button saveButton = (Button) ReflectionTestUtils.getField(view, "saveButton");
        Button cancelButton = (Button) ReflectionTestUtils.getField(view, "cancelButton");
        assertFalse(editButton.isVisible());
        assertTrue(saveButton.isVisible());
        assertTrue(cancelButton.isVisible());

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertEquals(1, editors.size());

        ReflectionTestUtils.invokeMethod(view, "addNewEntry");
        assertEquals(2, editors.size());

        DocumentationEntryEditor editorMock = mock(DocumentationEntryEditor.class);
        when(editorMock.getEntry()).thenReturn(CompletableFuture.completedFuture(
                new DocumentationEntry("chapter-2", "chapter", "Kapitola 2", "{}", List.of())
        ));
        editors.clear();
        editors.add(editorMock);

        ReflectionTestUtils.invokeMethod(view, "saveChanges");

        verify(documentationService).saveAll(anyList());
    }

    @Test
    void setupEditorContentShouldDifferentiateJsonAndHtmlAndCancelEditShouldResetState() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntries()).thenReturn(List.of(new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of())));

        DocumentationView view = new DocumentationView(documentationService);
        UI.getCurrent().add(view);

        FakeEditorJs jsonEditor = new FakeEditorJs();
        ReflectionTestUtils.invokeMethod(view, "setupEditorContent", jsonEditor, "{\"blocks\":[]}");
        assertEquals("{\"blocks\":[]}", jsonEditor.chapterContentData);

        FakeEditorJs htmlEditor = new FakeEditorJs();
        ReflectionTestUtils.invokeMethod(view, "setupEditorContent", htmlEditor, "<p>Hello</p>");
        assertEquals("<p>Hello</p>", htmlEditor.htmlContent);

        ReflectionTestUtils.setField(view, "editMode", true);
        ReflectionTestUtils.invokeMethod(view, "cancelEdit");
        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
    }

    @Test
    void nonAdminShouldIgnoreEnterEditMode() {
        when(documentationService.getEntries()).thenReturn(List.of(new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of())));

        DocumentationView view = new DocumentationView(documentationService);
        UI.getCurrent().add(view);

        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
        assertFalse(((Button) ReflectionTestUtils.getField(view, "editButton")).isVisible());
    }

    @Test
    void showEntryShouldSwitchToSingleEntryMode() {
        DocumentationEntry entry = new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of());
        when(documentationService.getEntries()).thenReturn(List.of(entry));

        DocumentationView view = new DocumentationView(documentationService);
        UI.getCurrent().add(view);

        ReflectionTestUtils.invokeMethod(view, "showEntry", entry);

        assertEquals(entry, ReflectionTestUtils.getField(view, "currentEntry"));
        assertTrue(findAll(view, Button.class).stream().anyMatch(button -> "Úvod".equals(button.getText())));
    }

    private static final class FakeEditorJs extends EditorJs {
        private String chapterContentData;
        private String htmlContent;

        private FakeEditorJs() {
            super(false);
        }

        @Override
        public void setChapterContentData(String jsonData) {
            this.chapterContentData = jsonData;
        }

        @Override
        public void loadMoodleHtml(String html) {
            this.htmlContent = html;
        }
    }
}
