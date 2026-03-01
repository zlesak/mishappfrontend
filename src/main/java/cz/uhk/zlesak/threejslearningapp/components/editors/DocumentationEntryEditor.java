package cz.uhk.zlesak.threejslearningapp.components.editors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *  UI component for editing a single {@link DocumentationEntry}.
 * It encapsulates fields for metadata (ID, Type, Title, Roles) and an integrated Editor.js instance for content manipulation.
 */
@Slf4j
public class DocumentationEntryEditor extends VerticalLayout implements I18nAware {

    private final TextField idField = new TextField(text("doc.editor.id"));
    private final ComboBox<String> typeSelect = new ComboBox<>(text("doc.editor.type"));
    private final TextField titleField = new TextField(text("doc.editor.title"));
    private final CheckboxGroup<String> rolesField = new CheckboxGroup<>(text("doc.editor.roles"));
    private final EditorJs editor = new EditorJs(true);

    @Getter
    private final Button removeButton = new Button(text("doc.editor.remove"));

    private static final List<String> ALL_ROLES = Arrays.asList(
            "ROLE_ADMIN",
            "ROLE_TEACHER",
            "ROLE_STUDENT"
    );

    private static final List<String> ENTRY_TYPES = Arrays.asList("chapter", "model", "quiz", "basic_info");

    /**
     * Constructs a new editor instance.
     *
     * @param lockedType If provided, the type selection will be restricted to this value.
     */
    public DocumentationEntryEditor(String lockedType) {
        setSpacing(true);
        setPadding(true);
        setWidthFull();

        typeSelect.setItems(ENTRY_TYPES);
        typeSelect.setWidthFull();

        titleField.setWidthFull();
        titleField.setPlaceholder(text("doc.editor.title.placeholder"));

        rolesField.setItems(ALL_ROLES);

        removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        if (lockedType != null) {
            typeSelect.setValue(lockedType);
            typeSelect.setReadOnly(true);
        }

        editor.setWidthFull();
        editor.setMinHeight("300px");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setMargin(false);
        horizontalLayout.add(typeSelect, titleField);
        horizontalLayout.setWidthFull();

        add(horizontalLayout, rolesField, editor, removeButton);
    }

    /**
     * Populates the editor fields with data from the provided entry.
     *
     * @param entry The documentation entry to be edited.
     */
    public void setEntry(DocumentationEntry entry) {
        if (entry == null) return;

        idField.setValue(entry.getId() != null ? entry.getId() : "");
        typeSelect.setValue(entry.getType());
        titleField.setValue(entry.getTitle() != null ? entry.getTitle() : "");

        rolesField.deselectAll();
        if (entry.getRoles() != null) {
            rolesField.select(entry.getRoles());
        }

        if (entry.getContent() != null) {
            editor.setChapterContentData(entry.getContent());
        } else {
            editor.setChapterContentData("{}");
        }
    }

    /**
     * Collects the current state of the UI fields and asynchronous editor data
     * into a new {@link DocumentationEntry} object.
     *
     * @return A CompletableFuture containing the updated DocumentationEntry.
     */
    public CompletableFuture<DocumentationEntry> getEntry() {
        return editor.getData().handle((json, ex) -> {
            DocumentationEntry updatedEntry = new DocumentationEntry();
            updatedEntry.setId(idField.getValue());
            updatedEntry.setType(typeSelect.getValue());
            updatedEntry.setTitle(titleField.getValue());
            updatedEntry.setRoles(rolesField.getSelectedItems().stream().toList());

            if (ex != null || json == null) {
                log.error("Failed to retrieve JSON data from EditorJs for entry: {}", updatedEntry.getId());
                updatedEntry.setContent("{}");
            } else {
                updatedEntry.setContent(json);
            }

            return updatedEntry;
        });
    }
}