package cz.uhk.zlesak.threejslearningapp.components.editors;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationEntryEditorTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldLockTypeWhenProvided() {
        DocumentationEntryEditor editor = new DocumentationEntryEditor("chapter");

        ComboBox<String> typeSelect = comboBox(getField(editor, "typeSelect", ComboBox.class));
        assertEquals("chapter", typeSelect.getValue());
        assertTrue(typeSelect.isReadOnly());
    }

    @Test
    void setEntryShouldPopulateFieldsAndFallbackContent() {
        DocumentationEntryEditor editor = new DocumentationEntryEditor(null);
        FakeEditorJs fakeEditor = replaceEditor(editor);
        DocumentationEntry entry = new DocumentationEntry("doc-1", "quiz", "Nadpis", null, List.of("ROLE_ADMIN", "ROLE_STUDENT"));

        editor.setEntry(entry);

        assertEquals("doc-1", getField(editor, "idField", TextField.class).getValue());
        assertEquals("quiz", getField(editor, "typeSelect", ComboBox.class).getValue());
        assertEquals("Nadpis", getField(editor, "titleField", TextField.class).getValue());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_STUDENT"), getField(editor, "rolesField", CheckboxGroup.class).getSelectedItems().stream().sorted().toList());
        assertEquals("{}", fakeEditor.lastContent);
    }

    @Test
    void getEntryShouldUseEditorDataWhenAvailable() {
        DocumentationEntryEditor editor = new DocumentationEntryEditor(null);
        FakeEditorJs fakeEditor = replaceEditor(editor);
        fakeEditor.dataFuture = CompletableFuture.completedFuture("{\"blocks\":[]}");

        getField(editor, "idField", TextField.class).setValue("entry-2");
        comboBox(getField(editor, "typeSelect", ComboBox.class)).setValue("model");
        getField(editor, "titleField", TextField.class).setValue("Model title");
        CheckboxGroup<String> rolesField = checkboxGroup(getField(editor, "rolesField", CheckboxGroup.class));
        rolesField.setValue(Set.of("ROLE_TEACHER"));

        DocumentationEntry result = editor.getEntry().join();

        assertEquals("entry-2", result.getId());
        assertEquals("model", result.getType());
        assertEquals("Model title", result.getTitle());
        assertEquals(List.of("ROLE_TEACHER"), result.getRoles());
        assertEquals("{\"blocks\":[]}", result.getContent());
    }

    @Test
    void getEntryShouldFallbackToEmptyJsonOnEditorFailure() {
        DocumentationEntryEditor editor = new DocumentationEntryEditor(null);
        FakeEditorJs fakeEditor = replaceEditor(editor);
        fakeEditor.dataFuture = CompletableFuture.failedFuture(new IllegalStateException("editor failed"));

        getField(editor, "idField", TextField.class).setValue("entry-3");
        DocumentationEntry result = editor.getEntry().join();

        assertEquals("entry-3", result.getId());
        assertEquals("{}", result.getContent());
    }

    private FakeEditorJs replaceEditor(DocumentationEntryEditor editor) {
        try {
            FakeEditorJs fakeEditor = new FakeEditorJs();
            Field field = DocumentationEntryEditor.class.getDeclaredField("editor");
            field.setAccessible(true);
            field.set(editor, fakeEditor);
            return fakeEditor;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    private <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class FakeEditorJs extends EditorJs {
        private String lastContent;
        private CompletableFuture<String> dataFuture = CompletableFuture.completedFuture("{}");

        private FakeEditorJs() {
            super(true);
        }

        @Override
        public void setChapterContentData(String jsonData) {
            this.lastContent = jsonData;
        }

        @Override
        public CompletableFuture<String> getData() {
            return dataFuture;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ComboBox<T> comboBox(Object raw) {
        return (ComboBox<T>) raw;
    }

    @SuppressWarnings("unchecked")
    private static <T> CheckboxGroup<T> checkboxGroup(Object raw) {
        return (CheckboxGroup<T>) raw;
    }
}
