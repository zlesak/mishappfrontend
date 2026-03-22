package cz.uhk.zlesak.threejslearningapp.views.documentation;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.components.editors.DocumentationEntryEditor;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntryIndex;
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
import java.util.concurrent.Executor;

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
        when(documentationService.getEntryIndexes()).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        List<Span> spans = findAll(view, Span.class);
        assertTrue(spans.stream().anyMatch(span -> "Žádné položky k zobrazení.".equals(span.getText())));
    }

    @Test
    void shouldLoadChapterCategoryByDefaultAfterOpen() {
        DocumentationEntry chapterEntry = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList()))
                .thenReturn(List.of(chapterEntry.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();

        verify(documentationService).getEntryIndexesByTypeForRoles(eq("chapter"), anyList());
        assertEquals("chapter", ReflectionTestUtils.getField(view, "currentFilterType"));
    }

    @Test
    void filterButtonsShouldReloadEntriesByType() {
        DocumentationEntry allEntry = new DocumentationEntry("all-1", "chapter", "Úvod", "{}", List.of());
        DocumentationEntry chapterEntry = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntryIndex allIndex = allEntry.toIndex();
        DocumentationEntryIndex chapterIndex = chapterEntry.toIndex();

        when(documentationService.getEntryIndexes()).thenReturn(List.of(allIndex));
        when(documentationService.getEntryIndexesByType("chapter")).thenReturn(List.of(chapterIndex));
        when(documentationService.getEntryDetail("all-1")).thenReturn(allEntry);
        when(documentationService.getEntryDetail("chapter-1")).thenReturn(chapterEntry);

        DocumentationView view = createViewWithSynchronousExecutor();

        assertTrue(findAll(view, Button.class).stream().anyMatch(button -> "--- Zobrazit vše ---".equals(button.getText())));
        assertTrue(findAll(view, Button.class).stream().anyMatch(button -> "Úvod".equals(button.getText())));

        findButtonByText(view, "Kapitoly").click();

        verify(documentationService).getEntryIndexesByType("chapter");
        List<Button> buttons = findAll(view, Button.class);
        assertEquals(1, buttons.stream().filter(button -> "Kapitola 1".equals(button.getText())).count());
    }

    @Test
    void adminModeShouldEnterEditAddEntryAndSaveMergedEntries() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        DocumentationEntry chapterEntry = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry hiddenModelEntry = new DocumentationEntry("model-hidden", "model", "Model hidden", "{}", List.of("ROLE_TEACHER"));
        DocumentationEntryIndex chapterIndex = chapterEntry.toIndex();
        when(documentationService.getEntryIndexes()).thenReturn(List.of(chapterIndex));
        when(documentationService.getEntryIndexesByType("chapter")).thenReturn(List.of(chapterIndex));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(chapterIndex));
        when(documentationService.getEntryDetail("chapter-1")).thenReturn(chapterEntry);
        when(documentationService.getEntryDetailForRoles(eq("chapter-1"), anyList())).thenReturn(chapterEntry);
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of(chapterEntry));
        when(documentationService.getAllEntriesForSave()).thenReturn(List.of(chapterEntry, hiddenModelEntry));

        DocumentationView view = createViewWithSynchronousExecutor();

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

        ReflectionTestUtils.setField(view, "currentFilterType", null);
        ReflectionTestUtils.invokeMethod(view, "addNewEntry");
        assertEquals(1, editors.size());

        ReflectionTestUtils.setField(view, "currentFilterType", "chapter");
        ReflectionTestUtils.invokeMethod(view, "addNewEntry");
        assertEquals(2, editors.size());

        DocumentationEntryEditor editorMock = mock(DocumentationEntryEditor.class);
        when(editorMock.getEntry()).thenReturn(CompletableFuture.completedFuture(
                new DocumentationEntry("chapter-2", "chapter", "Kapitola 2", "{}", List.of())
        ));
        editors.clear();
        editors.add(editorMock);

        ReflectionTestUtils.invokeMethod(view, "saveChanges");

        verify(documentationService, timeout(1000)).saveAll(anyList());
        verify(documentationService).saveAll(argThat(entries ->
                entries.stream().anyMatch(entry -> "model-hidden".equals(entry.getId()))
                        && entries.stream().anyMatch(entry -> "chapter-2".equals(entry.getId()))
        ));
    }

    @Test
    void setupEditorContentShouldDifferentiateJsonAndHtmlAndCancelEditShouldResetState() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexes()).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

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
        when(documentationService.getEntryIndexes()).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
        assertFalse(((Button) ReflectionTestUtils.getField(view, "editButton")).isVisible());
    }

    @Test
    void showEntryShouldSwitchToSingleEntryMode() {
        DocumentationEntry entry = new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of());
        DocumentationEntryIndex entryIndex = entry.toIndex();
        when(documentationService.getEntryIndexes()).thenReturn(List.of(entryIndex));
        when(documentationService.getEntryDetail("e-1")).thenReturn(entry);

        DocumentationView view = createViewWithSynchronousExecutor();

        ReflectionTestUtils.invokeMethod(view, "showEntry", "e-1", "Úvod");

        assertEquals("e-1", ReflectionTestUtils.getField(view, "currentEntryId"));
        assertTrue(findAll(view, Button.class).stream().anyMatch(button -> "Úvod".equals(button.getText())));
    }

    @Test
    void selectedEntryEditShouldSaveOnlySelectedEntryAndKeepOtherCategoryItems() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));

        DocumentationEntry chapter1 = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry chapter2 = new DocumentationEntry("chapter-2", "chapter", "Kapitola 2", "{}", List.of());
        DocumentationEntry modelHidden = new DocumentationEntry("model-hidden", "model", "Model hidden", "{}", List.of("ROLE_TEACHER"));
        DocumentationEntryIndex chapter1Index = chapter1.toIndex();
        DocumentationEntryIndex chapter2Index = chapter2.toIndex();

        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList()))
                .thenReturn(List.of(chapter1Index, chapter2Index));
        when(documentationService.getEntryDetailForRoles(eq("chapter-1"), anyList())).thenReturn(chapter1);
        when(documentationService.getAllEntriesForSave()).thenReturn(List.of(chapter1, chapter2, modelHidden));

        DocumentationView view = createViewWithSynchronousExecutor();

        ReflectionTestUtils.invokeMethod(view, "showEntry", "chapter-1", "Kapitola 1");
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertEquals(1, editors.size());

        DocumentationEntryEditor editorMock = mock(DocumentationEntryEditor.class);
        when(editorMock.getEntry()).thenReturn(CompletableFuture.completedFuture(
                new DocumentationEntry("chapter-1", "chapter", "Kapitola 1 - upravená", "{}", List.of())
        ));
        editors.clear();
        editors.add(editorMock);

        ReflectionTestUtils.invokeMethod(view, "saveChanges");

        verify(documentationService, timeout(1000)).saveAll(argThat(entries ->
                entries.size() == 3
                        && entries.stream().anyMatch(entry -> "chapter-1".equals(entry.getId()) && "Kapitola 1 - upravená".equals(entry.getTitle()))
                        && entries.stream().anyMatch(entry -> "chapter-2".equals(entry.getId()) && "Kapitola 2".equals(entry.getTitle()))
                        && entries.stream().anyMatch(entry -> "model-hidden".equals(entry.getId()))
        ));
    }

    private DocumentationView createViewWithSynchronousExecutor() {
        DocumentationView view = new DocumentationView(documentationService);
        ReflectionTestUtils.setField(view, "ioExecutor", (Executor) Runnable::run);
        UI.getCurrent().add(view);
        return view;
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
