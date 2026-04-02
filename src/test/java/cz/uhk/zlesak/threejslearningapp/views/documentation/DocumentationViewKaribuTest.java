package cz.uhk.zlesak.threejslearningapp.views.documentation;

import com.github.mvysny.kaributesting.v10.MockVaadin;
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
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

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
        DocumentationEntry chapterEntry = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry modelEntry = new DocumentationEntry("model-1", "model", "Model 1", "{}", List.of());
        DocumentationEntryIndex chapterIndex = chapterEntry.toIndex();
        DocumentationEntryIndex modelIndex = modelEntry.toIndex();

        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(chapterIndex));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("model"), anyList())).thenReturn(List.of(modelIndex));

        DocumentationView view = createViewWithSynchronousExecutor();

        assertTrue(findAll(view, Button.class).stream().anyMatch(button -> "Kapitola 1".equals(button.getText())));

        findButtonByText(view, "Modely").click();
        MockVaadin.clientRoundtrip(false);

        verify(documentationService).getEntryIndexesByTypeForRoles(eq("model"), anyList());
        List<Button> buttons = findAll(view, Button.class);
        assertEquals(1, buttons.stream().filter(button -> "Model 1".equals(button.getText())).count());
    }

    @Test
    void adminModeShouldEnterEditAddEntryAndSaveMergedEntries() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        DocumentationEntry chapterEntry = new DocumentationEntry("chapter-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry hiddenModelEntry = new DocumentationEntry("model-hidden", "model", "Model hidden", "{}", List.of("ROLE_TEACHER"));
        DocumentationEntryIndex chapterIndex = chapterEntry.toIndex();
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(chapterIndex));
        when(documentationService.getEntryIndexesForRoles(anyList())).thenReturn(List.of());
        when(documentationService.getEntryDetail("chapter-1")).thenReturn(chapterEntry);
        when(documentationService.getEntryDetailForRoles(eq("chapter-1"), anyList())).thenReturn(chapterEntry);
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of(chapterEntry));
        when(documentationService.getAllEntriesForSave()).thenReturn(List.of(chapterEntry, hiddenModelEntry));

        DocumentationView view = createViewWithSynchronousExecutor();

        ReflectionTestUtils.invokeMethod(view, "navigateToType", "chapter");
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");
        MockVaadin.clientRoundtrip(false);

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
        MockVaadin.clientRoundtrip(false);

        verify(documentationService, timeout(1000)).saveAll(anyList());
        verify(documentationService).saveAll(argThat(entries ->
                entries.stream().anyMatch(entry -> "model-hidden".equals(entry.getId()))
                        && entries.stream().anyMatch(entry -> "chapter-2".equals(entry.getId()))
        ));
    }

    @Test
    void setupEditorContentShouldDifferentiateJsonAndHtmlAndCancelEditShouldResetState() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

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
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
        assertFalse(((Button) ReflectionTestUtils.getField(view, "editButton")).isVisible());
    }

    @Test
    void showEntryShouldSwitchToSingleEntryMode() {
        DocumentationEntry entry = new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of());
        DocumentationEntryIndex entryIndex = entry.toIndex();
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entryIndex));
        when(documentationService.getEntryDetailForRoles(eq("e-1"), anyList())).thenReturn(entry);

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
        when(documentationService.getEntryIndexesForRoles(anyList())).thenReturn(List.of());
        when(documentationService.getEntryDetailForRoles(eq("chapter-1"), anyList())).thenReturn(chapter1);
        when(documentationService.getAllEntriesForSave()).thenReturn(List.of(chapter1, chapter2, modelHidden));

        DocumentationView view = createViewWithSynchronousExecutor();

        ReflectionTestUtils.invokeMethod(view, "showEntry", "chapter-1", "Kapitola 1");
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");
        MockVaadin.clientRoundtrip(false);

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
        MockVaadin.clientRoundtrip(false);

        verify(documentationService, timeout(1000)).saveAll(argThat(entries ->
                entries.size() == 3
                        && entries.stream().anyMatch(entry -> "chapter-1".equals(entry.getId()) && "Kapitola 1 - upravená".equals(entry.getTitle()))
                        && entries.stream().anyMatch(entry -> "chapter-2".equals(entry.getId()) && "Kapitola 2".equals(entry.getTitle()))
                        && entries.stream().anyMatch(entry -> "model-hidden".equals(entry.getId()))
        ));
    }

    @Test
    void clickingModelFilterButtonShouldLoadModelEntries() {
        DocumentationEntry chapter = new DocumentationEntry("ch-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry model = new DocumentationEntry("m-1", "model", "Model 1", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(chapter.toIndex()));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("model"), anyList())).thenReturn(List.of(model.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();

        findButtonByText(view, "Modely").click();
        MockVaadin.clientRoundtrip(false);

        verify(documentationService).getEntryIndexesByTypeForRoles(eq("model"), anyList());
        assertTrue(findAll(view, Button.class).stream().anyMatch(b -> "Model 1".equals(b.getText())));
    }

    @Test
    void clickingQuizFilterButtonShouldLoadQuizEntries() {
        DocumentationEntry quiz = new DocumentationEntry("q-1", "quiz", "Kvíz 1", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("quiz"), anyList())).thenReturn(List.of(quiz.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();

        findButtonByText(view, "Kvízy").click();
        MockVaadin.clientRoundtrip(false);

        verify(documentationService).getEntryIndexesByTypeForRoles(eq("quiz"), anyList());
        assertTrue(findAll(view, Button.class).stream().anyMatch(b -> "Kvíz 1".equals(b.getText())));
    }

    @Test
    void filterButtonsExistForAllThreeTypes() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        List<Button> buttons = findAll(view, Button.class);
        assertTrue(buttons.stream().anyMatch(b -> "Kapitoly".equals(b.getText())));
        assertTrue(buttons.stream().anyMatch(b -> "Modely".equals(b.getText())));
        assertTrue(buttons.stream().anyMatch(b -> "Kvízy".equals(b.getText())));
    }

    @Test
    void clickingModelFilterShouldSetCurrentFilterTypeToModel() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("model"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        findButtonByText(view, "Modely").click();
        MockVaadin.clientRoundtrip(false);

        assertEquals("model", ReflectionTestUtils.getField(view, "currentFilterType"));
    }

    @Test
    void clickingQuizFilterShouldSetCurrentFilterTypeToQuiz() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("quiz"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        findButtonByText(view, "Kvízy").click();
        MockVaadin.clientRoundtrip(false);

        assertEquals("quiz", ReflectionTestUtils.getField(view, "currentFilterType"));
    }

    @Test
    void filterChangeResetsCurrentEntryId() {
        DocumentationEntry entry = new DocumentationEntry("ch-1", "chapter", "Kapitola 1", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entry.toIndex()));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("model"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "currentEntryId", "ch-1");

        findButtonByText(view, "Modely").click();
        MockVaadin.clientRoundtrip(false);

        assertNull(ReflectionTestUtils.getField(view, "currentEntryId"));
    }

    @Test
    void singleEntryRendersAsClickableButton() {
        DocumentationEntry entry = new DocumentationEntry("ch-1", "chapter", "Moje Kapitola", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entry.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();

        long count = findAll(view, Button.class).stream().filter(b -> "Moje Kapitola".equals(b.getText())).count();
        assertEquals(1, count);
    }

    @Test
    void multipleEntriesRenderAsMultipleButtons() {
        DocumentationEntry e1 = new DocumentationEntry("ch-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry e2 = new DocumentationEntry("ch-2", "chapter", "Kapitola 2", "{}", List.of());
        DocumentationEntry e3 = new DocumentationEntry("ch-3", "chapter", "Kapitola 3", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList()))
                .thenReturn(List.of(e1.toIndex(), e2.toIndex(), e3.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();

        long count = findAll(view, Button.class).stream()
                .filter(b -> b.getText().startsWith("Kapitola "))
                .count();
        assertEquals(3, count);
    }

    @Test
    void entriesWithNullRolesAreVisibleToStudent() {
        DocumentationEntry openEntry = new DocumentationEntry("open-1", "chapter", "Veřejná kapitola", "{}", null);
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(openEntry.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();

        assertTrue(findAll(view, Button.class).stream().anyMatch(b -> "Veřejná kapitola".equals(b.getText())));
    }

    @Test
    void loadedEntriesAreStoredInCurrentEntryIndexes() {
        DocumentationEntry e1 = new DocumentationEntry("ch-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry e2 = new DocumentationEntry("ch-2", "chapter", "Kapitola 2", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList()))
                .thenReturn(List.of(e1.toIndex(), e2.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();

        @SuppressWarnings("unchecked")
        List<DocumentationEntryIndex> indexes = (List<DocumentationEntryIndex>) ReflectionTestUtils.getField(view, "currentEntryIndexes");
        assertEquals(2, indexes.size());
    }

    @Test
    void emptyEntryListRendersEmptyStateSpan() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertTrue(findAll(view, Span.class).stream().anyMatch(s -> "Žádné položky k zobrazení.".equals(s.getText())));
    }

    @Test
    void editButtonVisibleForAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertTrue(((Button) ReflectionTestUtils.getField(view, "editButton")).isVisible());
    }

    @Test
    void editButtonNotVisibleForStudent() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertFalse(((Button) ReflectionTestUtils.getField(view, "editButton")).isVisible());
    }

    @Test
    void saveButtonInitiallyHidden() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertFalse(((Button) ReflectionTestUtils.getField(view, "saveButton")).isVisible());
    }

    @Test
    void cancelButtonInitiallyHidden() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertFalse(((Button) ReflectionTestUtils.getField(view, "cancelButton")).isVisible());
    }

    @Test
    void adminEntersEditModeShouldShowSaveButton() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertTrue(((Button) ReflectionTestUtils.getField(view, "saveButton")).isVisible());
    }

    @Test
    void adminEntersEditModeShouldHideEditButton() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertFalse(((Button) ReflectionTestUtils.getField(view, "editButton")).isVisible());
    }

    @Test
    void adminEntersEditModeShouldShowCancelButton() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertTrue(((Button) ReflectionTestUtils.getField(view, "cancelButton")).isVisible());
    }

    @Test
    void addNewEntryWithValidTypeIncreasesEditorCount() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "currentFilterType", "chapter");
        ReflectionTestUtils.invokeMethod(view, "addNewEntry");

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertEquals(1, editors.size());
    }

    @Test
    void addNewEntryWithNullCurrentTypeDoesNotAddEditor() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "currentFilterType", null);
        ReflectionTestUtils.invokeMethod(view, "addNewEntry");

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertEquals(0, editors.size());
    }

    @Test
    void cancelEditSetsEditModeToFalse() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "editMode", true);
        ReflectionTestUtils.invokeMethod(view, "cancelEdit");

        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
    }

    @Test
    void cancelEditHidesSaveAndCancelButtons() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "editMode", true);
        ReflectionTestUtils.invokeMethod(view, "cancelEdit");

        assertFalse(((Button) ReflectionTestUtils.getField(view, "saveButton")).isVisible());
        assertFalse(((Button) ReflectionTestUtils.getField(view, "cancelButton")).isVisible());
    }

    @Test
    void showEntrySetsCurrentEntryIdField() {
        DocumentationEntry entry = new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entry.toIndex()));
        when(documentationService.getEntryDetailForRoles(eq("e-1"), anyList())).thenReturn(entry);

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "showEntry", "e-1", "Úvod");

        assertEquals("e-1", ReflectionTestUtils.getField(view, "currentEntryId"));
    }

    @Test
    void showEntryWithEmptyIdDoesNotSetCurrentEntryId() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "showEntry", "", "Title");

        assertNull(ReflectionTestUtils.getField(view, "currentEntryId"));
    }

    @Test
    void showEntryCallsGetEntryDetailForRoles() {
        DocumentationEntry entry = new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entry.toIndex()));
        when(documentationService.getEntryDetailForRoles(eq("e-1"), anyList())).thenReturn(entry);

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "showEntry", "e-1", "Úvod");

        verify(documentationService, atLeastOnce()).getEntryDetailForRoles(eq("e-1"), anyList());
    }

    @Test
    void showEntryExitsEditMode() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        DocumentationEntry entry = new DocumentationEntry("e-1", "chapter", "Úvod", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entry.toIndex()));
        when(documentationService.getEntryDetailForRoles(eq("e-1"), anyList())).thenReturn(entry);

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "editMode", true);
        ReflectionTestUtils.invokeMethod(view, "showEntry", "e-1", "Úvod");

        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
    }

    @Test
    void initialCurrentFilterTypeIsChapterAfterOpen() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertEquals("chapter", ReflectionTestUtils.getField(view, "currentFilterType"));
    }

    @Test
    void editModeFalseByDefault() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
    }

    @Test
    void currentEntryIdNullByDefault() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        assertNull(ReflectionTestUtils.getField(view, "currentEntryId"));
    }

    @Test
    void entryEditorListEmptyByDefault() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertTrue(editors.isEmpty());
    }

    @Test
    void enteringEditModeAsAdminSetsEditModeFieldToTrue() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertTrue((Boolean) ReflectionTestUtils.getField(view, "editMode"));
    }

    @Test
    void serviceReturningNullDoesNotCrash() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(null);

        DocumentationView view = createViewWithSynchronousExecutor();

        assertTrue(findAll(view, Span.class).stream().anyMatch(s -> "Žádné položky k zobrazení.".equals(s.getText())));
    }

    @Test
    void switchingBetweenFiltersMultipleTimesDoesNotCrash() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("model"), anyList())).thenReturn(List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("quiz"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        findButtonByText(view, "Modely").click();
        MockVaadin.clientRoundtrip(false);
        findButtonByText(view, "Kvízy").click();
        MockVaadin.clientRoundtrip(false);
        findButtonByText(view, "Kapitoly").click();
        MockVaadin.clientRoundtrip(false);

        assertEquals("chapter", ReflectionTestUtils.getField(view, "currentFilterType"));
    }

    @Test
    void adminSaveWithNoEditorsCallsSaveAll() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        DocumentationEntry chapter1 = new DocumentationEntry("ch-1", "chapter", "Kap 1", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getAllEntriesForSave()).thenReturn(List.of(chapter1));

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "saveChanges");
        MockVaadin.clientRoundtrip(false);

        verify(documentationService, timeout(1000)).saveAll(anyList());
    }

    @Test
    void enterEditModeWithNonAdminDoesNotSetEditMode() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        assertFalse((Boolean) ReflectionTestUtils.getField(view, "editMode"));
    }

    @Test
    void enterEditModeAsAdminShouldLoadEditorsForCurrentType() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        DocumentationEntry ch1 = new DocumentationEntry("ch-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry ch2 = new DocumentationEntry("ch-2", "chapter", "Kapitola 2", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList()))
                .thenReturn(List.of(ch1.toIndex(), ch2.toIndex()));
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of(ch1, ch2));

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");
        MockVaadin.clientRoundtrip(false);

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertEquals(2, editors.size());
    }

    @Test
    void setupEditorContentWithJsonDataSetsChapterContent() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        FakeEditorJs editor = new FakeEditorJs();
        ReflectionTestUtils.invokeMethod(view, "setupEditorContent", editor, "{\"blocks\":[{\"type\":\"paragraph\"}]}");

        assertEquals("{\"blocks\":[{\"type\":\"paragraph\"}]}", editor.chapterContentData);
    }

    @Test
    void setupEditorContentWithHtmlDataSetsHtmlContent() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        FakeEditorJs editor = new FakeEditorJs();
        ReflectionTestUtils.invokeMethod(view, "setupEditorContent", editor, "<h1>Title</h1>");

        assertEquals("<h1>Title</h1>", editor.htmlContent);
    }

    @Test
    void setupEditorContentWithNullUsesEmptyJson() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        FakeEditorJs editor = new FakeEditorJs();
        ReflectionTestUtils.invokeMethod(view, "setupEditorContent", editor, (Object) null);

        assertEquals("{}", editor.chapterContentData);
    }

    @Test
    void setupEditorContentWithArrayJsonSetsChapterContent() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        FakeEditorJs editor = new FakeEditorJs();
        ReflectionTestUtils.invokeMethod(view, "setupEditorContent", editor, "[{\"type\":\"paragraph\"}]");

        assertEquals("[{\"type\":\"paragraph\"}]", editor.chapterContentData);
    }

    @Test
    void addTwoNewEntriesResultsInTwoEditors() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "currentFilterType", "chapter");
        ReflectionTestUtils.invokeMethod(view, "addNewEntry");
        ReflectionTestUtils.invokeMethod(view, "addNewEntry");

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertEquals(2, editors.size());
    }

    @Test
    void addNewEntryWithBlankCurrentTypeDoesNotAddEditor() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "currentFilterType", "  ");
        ReflectionTestUtils.invokeMethod(view, "addNewEntry");

        @SuppressWarnings("unchecked")
        List<DocumentationEntryEditor> editors = (List<DocumentationEntryEditor>) ReflectionTestUtils.getField(view, "entryEditors");
        assertEquals(0, editors.size());
    }

    @Test
    void cancelEditAfterNavigatingToChapterShouldReloadChapterList() {
        DocumentationEntry entry = new DocumentationEntry("ch-1", "chapter", "Kapitola 1", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entry.toIndex()));

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "editMode", true);
        ReflectionTestUtils.invokeMethod(view, "cancelEdit");

        verify(documentationService, atLeast(2)).getEntryIndexesByTypeForRoles(eq("chapter"), anyList());
    }

    @Test
    void showEntryRendersEntryTitleInContent() {
        DocumentationEntry entry = new DocumentationEntry("e-1", "chapter", "Moje Položka", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of(entry.toIndex()));
        when(documentationService.getEntryDetailForRoles(eq("e-1"), anyList())).thenReturn(entry);

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "showEntry", "e-1", "Moje Položka");

        assertTrue(findAll(view, Span.class).stream().anyMatch(s -> "Moje Položka".equals(s.getText())));
    }

    @Test
    void filterChangeCallsServiceWithCorrectTypeEachTime() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("model"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();

        findButtonByText(view, "Modely").click();
        MockVaadin.clientRoundtrip(false);

        verify(documentationService, times(1)).getEntryIndexesByTypeForRoles(eq("chapter"), anyList());
        verify(documentationService, times(1)).getEntryIndexesByTypeForRoles(eq("model"), anyList());
    }

    @Test
    void viewInitiallyLoadsChapterTypeOnAttach() {
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        createViewWithSynchronousExecutor();

        verify(documentationService).getEntryIndexesByTypeForRoles(eq("chapter"), anyList());
    }

    @Test
    void showEntryMultipleTimesUpdatesCurrentEntryId() {
        DocumentationEntry e1 = new DocumentationEntry("e-1", "chapter", "Kapitola 1", "{}", List.of());
        DocumentationEntry e2 = new DocumentationEntry("e-2", "chapter", "Kapitola 2", "{}", List.of());
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList()))
                .thenReturn(List.of(e1.toIndex(), e2.toIndex()));
        when(documentationService.getEntryDetailForRoles(eq("e-1"), anyList())).thenReturn(e1);
        when(documentationService.getEntryDetailForRoles(eq("e-2"), anyList())).thenReturn(e2);

        DocumentationView view = createViewWithSynchronousExecutor();

        ReflectionTestUtils.invokeMethod(view, "showEntry", "e-1", "Kapitola 1");
        assertEquals("e-1", ReflectionTestUtils.getField(view, "currentEntryId"));

        ReflectionTestUtils.invokeMethod(view, "showEntry", "e-2", "Kapitola 2");
        assertEquals("e-2", ReflectionTestUtils.getField(view, "currentEntryId"));
    }

    @Test
    void adminEnterEditModeCallsGetAllEntriesByTypeForSave() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());
        when(documentationService.getAllEntriesByTypeForSave("chapter")).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.invokeMethod(view, "enterEditMode");

        verify(documentationService).getAllEntriesByTypeForSave("chapter");
    }

    @Test
    void currentFilterTypeRemainsChapterAfterCancelEdit() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));
        when(documentationService.getEntryIndexesByTypeForRoles(eq("chapter"), anyList())).thenReturn(List.of());

        DocumentationView view = createViewWithSynchronousExecutor();
        ReflectionTestUtils.setField(view, "editMode", true);
        ReflectionTestUtils.invokeMethod(view, "cancelEdit");

        assertEquals("chapter", ReflectionTestUtils.getField(view, "currentFilterType"));
    }

    private DocumentationView createViewWithSynchronousExecutor() {
        DocumentationView view = new DocumentationView(documentationService);
        ReflectionTestUtils.setField(view, "ioExecutor", (Executor) Runnable::run);
        UI.getCurrent().add(view);
        MockVaadin.clientRoundtrip(false);
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
