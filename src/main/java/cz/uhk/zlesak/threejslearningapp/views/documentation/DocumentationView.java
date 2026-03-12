package cz.uhk.zlesak.threejslearningapp.views.documentation;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.editors.DocumentationEntryEditor;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.services.DocumentationService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * View for displaying and managing application documentation.
 * Supports hierarchical filtering, single/multiple entry display modes, and a bulk administrative editor.
 */
@Slf4j
@Route("documentation")
@Tag("documentation-view")
@Scope("prototype")
@PermitAll
public class DocumentationView extends AbstractView<DocumentationService>  {

    private final DocumentationService documentationService;
    private final VerticalLayout sidebar;
    private final VerticalLayout entriesListLayout;
    private final Div contentContainer;
    private final VerticalLayout entriesEditContainer = new VerticalLayout();
    private final List<DocumentationEntryEditor> entryEditors = new ArrayList<>();

    private final Button editButton, saveButton,cancelButton, addEntryButton;

    private String currentFilterType = null;
    private boolean editMode = false;
    private DocumentationEntry currentEntry = null;
    private final Map<String, Button> filterButtons = new HashMap<>();
    @Autowired
    public DocumentationView(DocumentationService documentationService) {
        super("page.title.documentation", documentationService);
        this.documentationService = documentationService;

        sidebar = new VerticalLayout();
        sidebar.setWidth("300px");
        sidebar.setPadding(false);
        sidebar.getStyle().set("overflow", "auto");
        sidebar.addClassNames(LumoUtility.Padding.Right.MEDIUM);
        sidebar.setHeightFull();

        entriesListLayout = new VerticalLayout();
        entriesListLayout.setPadding(false);
        entriesListLayout.setSpacing(false);
        entriesListLayout.setWidthFull();

        contentContainer = new Div();
        contentContainer.setSizeFull();

        editButton = new Button(text("doc.admin.edit"), e -> enterEditMode());
        saveButton = new Button(text("doc.admin.save"), e -> saveChanges());
        cancelButton = new Button(text("doc.admin.cancel"), e -> cancelEdit());
        addEntryButton = new Button(text("doc.admin.add"));
        addEntryButton.addClickListener(e -> addNewEntry());

        saveButton.setVisible(false);
        cancelButton.setVisible(false);
        editButton.setVisible(isCurrentUserAdmin());

        configureLayout();
        loadList(null);
    }

    /**
     * Checks if the currently authenticated user holds administrative privileges.
     * @return true if user is admin.
     */
    private boolean isCurrentUserAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return false;
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ADMINISTRATOR"));
        } catch (Exception e) {
            log.warn("Security context access error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sets up the main layout structure including sidebar, top navigation, and content area.
     */
    private void configureLayout() {
        HorizontalLayout mainWrapper = new HorizontalLayout();
        mainWrapper.setSizeFull();
        mainWrapper.setPadding(false);
        mainWrapper.setSpacing(false);

        VerticalLayout centerLayout = new VerticalLayout();
        centerLayout.setSizeFull();
        centerLayout.setPadding(false);
        centerLayout.setSpacing(false);

        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setSpacing(true);

        createFilterButton(navigation, text("doc.nav.all"), null);
        createFilterButton(navigation, text("doc.nav.chapters"), "chapter");
        createFilterButton(navigation, text("doc.nav.models"), "model");
        createFilterButton(navigation, text("doc.nav.quizzes"), "quiz");

        HorizontalLayout adminArea = new HorizontalLayout(editButton, saveButton, cancelButton);
        adminArea.setSpacing(true);

        HorizontalLayout topBar = new HorizontalLayout(navigation, adminArea);
        topBar.setWidthFull();
        topBar.expand(navigation);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);

        sidebar.add(entriesListLayout);
        centerLayout.add(topBar, contentContainer);
        centerLayout.expand(contentContainer);

        mainWrapper.add(sidebar, centerLayout);
        mainWrapper.expand(centerLayout);

        getContent().add(mainWrapper);
        updateFilterButtonsSelection();
    }

    /**
     * Resets current selection and reloads list based on entry type.
     * @param type filter category.
     */
    private void navigateToType(String type) {
        this.currentEntry = null;
        this.currentFilterType = type;
        updateFilterButtonsSelection();
        loadList(type);
    }

    /**
     * Helper to create a navigation button and store it for state updates.
     */
    private void createFilterButton(HorizontalLayout layout, String label, String type) {
        Button btn = new Button(label, e -> navigateToType(type));
        filterButtons.put(String.valueOf(type), btn);
        layout.add(btn);
    }

    /**
     * Updates the visual state of filter buttons based on currentFilterType.
     */
    private void updateFilterButtonsSelection() {
        filterButtons.forEach((type, btn) -> {
            if (type.equals(String.valueOf(currentFilterType))) {
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            } else {
                btn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
        });
    }

    /**
     * Fetches entries from service and populates the sidebar navigation.
     * @param type filter category.
     */
    private void loadList(String type) {
        this.currentFilterType = type;
        List<DocumentationEntry> entries = (type == null)
                ? documentationService.getEntries()
                : documentationService.getEntriesByType(type);

        entriesListLayout.removeAll();

        if (entries.isEmpty()) {
            entriesListLayout.add(new Span(text("doc.list.empty")));
            contentContainer.removeAll();
            return;
        }

        Button showAllBtn = new Button(text("doc.list.showAll"));
        showAllBtn.setWidthFull();
        showAllBtn.addThemeVariants(currentEntry == null ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_CONTRAST);
        showAllBtn.addClickListener(e -> {
            this.currentEntry = null;
            showMultipleEntries(entries);
            loadList(currentFilterType);
        });
        entriesListLayout.add(showAllBtn);

        for (DocumentationEntry entry : entries) {
            Button entryBtn = new Button(entry.getTitle());
            entryBtn.setWidthFull();
            if (currentEntry != null && entry.getId().equals(currentEntry.getId())) {
                entryBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            entryBtn.addClickListener(e -> {
                showEntry(entry);
                loadList(currentFilterType);
            });
            entriesListLayout.add(entryBtn);
        }

        if (currentEntry == null) {
            showMultipleEntries(entries);
        }
    }

    /**
     * Renders multiple documentation entries sequentially in the content area.
     * @param entries list of entries to display.
     */
    private void showMultipleEntries(List<DocumentationEntry> entries) {
        this.currentEntry = null;
        exitEditModeUI();
        contentContainer.removeAll();

        VerticalLayout scrollLayout = new VerticalLayout();
        scrollLayout.setWidthFull();
        scrollLayout.setPadding(true);

        for (DocumentationEntry entry : entries) {
            scrollLayout.add(createSectionHeader(entry.getTitle()));
            EditorJs viewer = new EditorJs(false);
            viewer.setWidthFull();
            setupEditorContent(viewer, entry.getContent());
            scrollLayout.add(viewer);
        }
        contentContainer.add(scrollLayout);
    }

    /**
     * Creates a styled header component for documentation sections.
     * @param titleText text to display.
     * @return Div containing the styled header.
     */
    private Div createSectionHeader(String titleText) {
        Div header = new Div();
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.Padding.Bottom.SMALL
        );
        header.getStyle().set("border-bottom", "2px solid var(--lumo-contrast-10pct)");

        Span title = new Span(titleText);
        title.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.PRIMARY);

        header.add(title);
        return header;
    }

    /**
     * Renders a single documentation entry in detail mode.
     * @param entry entry to display.
     */
    private void showEntry(DocumentationEntry entry) {
        if (entry == null) return;
        this.currentEntry = entry;
        exitEditModeUI();
        contentContainer.removeAll();

        VerticalLayout detailLayout = new VerticalLayout();
        detailLayout.setSizeFull();
        detailLayout.setPadding(true);

        EditorJs viewerEditor = new EditorJs(false);
        viewerEditor.setSizeFull();
        setupEditorContent(viewerEditor, entry.getContent());

        detailLayout.add(createSectionHeader(entry.getTitle()), viewerEditor);
        detailLayout.expand(viewerEditor);
        contentContainer.add(detailLayout);
    }

    /**
     * Helper to determine content format and load it into an EditorJs instance.
     * @param editor target editor.
     * @param content raw string content.
     */
    private void setupEditorContent(EditorJs editor, String content) {
        String data = (content != null) ? content.trim() : "{}";
        if (data.startsWith("{") || data.startsWith("[")) {
            editor.setChapterContentData(data);
        } else {
            editor.loadMoodleHtml(data);
        }
    }

    /**
     * Switches the view to administrative edit mode.
     */
    private void enterEditMode() {
        if (!isCurrentUserAdmin()) return;
        editMode = true;

        editButton.setVisible(false);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);

        contentContainer.removeAll();
        entriesEditContainer.removeAll();
        entryEditors.clear();

        List<DocumentationEntry> entries = (currentFilterType == null)
                ? documentationService.getEntries()
                : documentationService.getEntriesByType(currentFilterType);

        entries.forEach(this::addEntryEditor);

        addEntryButton.setIcon(VaadinIcon.PLUS.create());

        contentContainer.add(entriesEditContainer, addEntryButton);
    }

    /**
     * Instantiates a new documentation entry and adds its editor to the container.
     */
    private void addNewEntry() {
        String id = currentFilterType + "-" + System.nanoTime();
        DocumentationEntry entry = new DocumentationEntry(id, currentFilterType, "", "{}", List.of());
        addEntryEditor(entry);
    }

    /**
     * Wraps an entry in a DocumentationEntryEditor and attaches removal logic.
     * @param entry entry to edit.
     */
    private void addEntryEditor(DocumentationEntry entry) {
        DocumentationEntryEditor editor = new DocumentationEntryEditor(currentFilterType);
        editor.setEntry(entry);
        editor.getRemoveButton().addClickListener(e -> {
            entriesEditContainer.remove(editor);
            entryEditors.remove(editor);
        });
        entryEditors.add(editor);
        entriesEditContainer.add(editor);
    }

    /**
     * Restores the UI to viewing mode.
     */
    private void exitEditModeUI() {
        if (!editMode) return;
        editMode = false;

        editButton.setVisible(isCurrentUserAdmin());
        saveButton.setVisible(false);
        cancelButton.setVisible(false);

        contentContainer.removeAll();
        if (currentEntry != null) {
            showEntry(currentEntry);
        }
    }

    /**
     * Discards changes and exits edit mode.
     */
    private void cancelEdit() {
        exitEditModeUI();
        loadList(currentFilterType);
    }

    /**
     * Collects data from all active editors and persists them via the service.
     */
    private void saveChanges() {
        List<DocumentationEntry> allEntries = documentationService.getEntries();
        List<CompletableFuture<DocumentationEntry>> futures = entryEditors.stream()
                .map(DocumentationEntryEditor::getEntry)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    List<DocumentationEntry> edited = futures.stream()
                            .map(CompletableFuture::join)
                            .toList();

                    List<DocumentationEntry> others = allEntries.stream()
                            .filter(e -> currentFilterType != null && !currentFilterType.equalsIgnoreCase(e.getType()))
                            .toList();

                    List<DocumentationEntry> merged = new ArrayList<>(others);
                    merged.addAll(edited);

                    documentationService.saveAll(merged);

                    getUI().ifPresent(ui -> ui.access(() -> {
                        new SuccessNotification(text("doc.notification.saved"));
                        exitEditModeUI();
                        loadList(currentFilterType);
                    }));
                });
    }
}
