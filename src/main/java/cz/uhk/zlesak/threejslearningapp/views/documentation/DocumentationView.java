package cz.uhk.zlesak.threejslearningapp.views.documentation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
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
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntryIndex;
import cz.uhk.zlesak.threejslearningapp.services.DocumentationService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private static final int DETAIL_CACHE_MAX_SIZE = Integer.parseInt(System.getenv().getOrDefault("FE_DOC_DETAIL_CACHE_MAX", "64"));
    private static final int PREFETCH_NEIGHBORS = Integer.parseInt(System.getenv().getOrDefault("FE_DOC_PREFETCH_NEIGHBORS", "1"));
    private static final String DEFAULT_FILTER_TYPE = "chapter";

    private final DocumentationService documentationService;
    private final VerticalLayout sidebar;
    private final VerticalLayout entriesListLayout;
    private final Div contentContainer;
    private final VerticalLayout entriesEditContainer = new VerticalLayout();
    private final List<DocumentationEntryEditor> entryEditors = new ArrayList<>();

    private final Button editButton, saveButton,cancelButton, addEntryButton;

    private String currentFilterType = null;
    private boolean editMode = false;
    private String currentEntryId = null;
    private List<DocumentationEntryIndex> currentEntryIndexes = List.of();
    private final AtomicInteger detailLoadSequence = new AtomicInteger(0);
    private final Set<String> prefetchInProgress = new HashSet<>();
    private final Map<String, DocumentationEntry> detailCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, DocumentationEntry> eldest) {
            return size() > DETAIL_CACHE_MAX_SIZE;
        }
    };
    private final Map<String, Button> filterButtons = new HashMap<>();
    private final AtomicInteger listLoadSequence = new AtomicInteger(0);
    private boolean initialListLoaded = false;

    @Autowired
    public DocumentationView(DocumentationService documentationService) {
        super("page.title.documentation", documentationService);
        this.documentationService = documentationService;

        sidebar = new VerticalLayout();
        sidebar.addClassName("documentation-sidebar");
        sidebar.setWidth(null);
        sidebar.setPadding(false);
        sidebar.getStyle().set("flex", "0 0 300px");
        sidebar.getStyle().set("max-width", "300px");
        sidebar.getStyle().set("overflow", "auto");
        sidebar.addClassNames(LumoUtility.Padding.Right.MEDIUM);
        sidebar.setHeightFull();

        entriesListLayout = new VerticalLayout();
        entriesListLayout.setPadding(false);
        entriesListLayout.setSpacing(false);
        entriesListLayout.setWidthFull();

        contentContainer = new Div();
        contentContainer.addClassName("documentation-content");
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
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!initialListLoaded) {
            initialListLoaded = true;
            // Open documentation with default category selected so sidebar is populated immediately.
            navigateToType(DEFAULT_FILTER_TYPE);
        }
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
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_ADMINISTRATOR".equals(a.getAuthority()));
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
        mainWrapper.addClassName("documentation-main-wrapper");
        mainWrapper.setSizeFull();
        mainWrapper.setPadding(false);
        mainWrapper.setSpacing(false);

        VerticalLayout centerLayout = new VerticalLayout();
        centerLayout.setSizeFull();
        centerLayout.setPadding(false);
        centerLayout.setSpacing(false);

        HorizontalLayout navigation = new HorizontalLayout();
        navigation.addClassName("documentation-nav");
        navigation.setSpacing(true);

        createFilterButton(navigation, text("doc.nav.chapters"), "chapter");
        createFilterButton(navigation, text("doc.nav.models"), "model");
        createFilterButton(navigation, text("doc.nav.quizzes"), "quiz");

        HorizontalLayout adminArea = new HorizontalLayout(editButton, saveButton, cancelButton);
        adminArea.addClassName("documentation-admin-actions");
        adminArea.setSpacing(true);

        HorizontalLayout topBar = new HorizontalLayout(navigation, adminArea);
        topBar.addClassName("documentation-topbar");
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
        this.currentEntryId = null;
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
        if (type == null) {
            renderIntroState();
            return;
        }

        final int requestId = listLoadSequence.incrementAndGet();
        final List<String> userRoles = resolveCurrentUserRoles();
        runAsync(() -> fetchEntryIndexesByType(type, userRoles), entries -> {
            if (requestId != listLoadSequence.get()) {
                return;
            }
            renderEntriesList(entries, true);
        }, error -> {
            if (requestId != listLoadSequence.get()) {
                return;
            }
            log.error("Documentation list loading failed for filter: {}", type, error);
            showErrorNotification(text("notification.loadError"), error);
        });
    }

    private void renderIntroState() {
        currentEntryIndexes = List.of();
        entriesListLayout.removeAll();
        entriesListLayout.add(new Span("Vyberte kategorii dokumentace."));

        contentContainer.removeAll();
        VerticalLayout intro = new VerticalLayout();
        intro.setPadding(true);
        intro.setSpacing(true);
        intro.add(
                new Span("Dokumentace je rozdělena do kategorií Kapitoly, Modely a Kvízy."),
                new Span("Pro pokračování vyberte kategorii v horní liště.")
        );
        contentContainer.add(intro);
    }

    private List<DocumentationEntryIndex> fetchEntryIndexesByType(String type, List<String> userRoles) {
        return type == null
                ? documentationService.getEntryIndexesForRoles(userRoles)
                : documentationService.getEntryIndexesByTypeForRoles(type, userRoles);
    }

    private List<DocumentationEntry> fetchEntriesForAdminEdit(String type) {
        return type == null
                ? documentationService.getAllEntriesForSave()
                : documentationService.getAllEntriesByTypeForSave(type);
    }

    private List<DocumentationEntry> fetchEntriesForAdminEditScope() {
        if (currentEntryId == null || currentEntryId.isBlank()) {
            return fetchEntriesForAdminEdit(currentFilterType);
        }

        return documentationService.getAllEntriesForSave().stream()
                .filter(entry -> currentEntryId.equals(entry.getId()))
                .toList();
    }

    private boolean isSingleEntryEditScope() {
        return currentEntryId != null && !currentEntryId.isBlank();
    }

    private void renderEntriesList(List<DocumentationEntryIndex> entries, boolean resetContentArea) {
        currentEntryIndexes = entries == null ? List.of() : List.copyOf(entries);
        entriesListLayout.removeAll();
        if (resetContentArea) {
            contentContainer.removeAll();
        }

        if (currentEntryIndexes.isEmpty()) {
            entriesListLayout.add(new Span(text("doc.list.empty")));
            contentContainer.add(new Span("V této kategorii zatím není žádná položka."));
            return;
        }

        if (currentEntryId != null && currentEntryIndexes.stream().noneMatch(e -> currentEntryId.equals(e.getId()))) {
            currentEntryId = null;
        }

        for (DocumentationEntryIndex entry : currentEntryIndexes) {
            Button entryBtn = new Button(entry.getTitle());
            entryBtn.setWidthFull();
            if (entry.getId().equals(currentEntryId)) {
                entryBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            entryBtn.addClickListener(e -> {
                showEntry(entry.getId(), entry.getTitle());
                renderEntriesList(currentEntryIndexes, false);
            });
            entriesListLayout.add(entryBtn);
        }

        if (resetContentArea && currentEntryId == null) {
            contentContainer.add(new Span("Vyberte položku dokumentace ze seznamu vlevo."));
        }
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
     * @param entryId documentation entry id.
     * @param fallbackTitle fallback title used before detail loads.
     */
    private void showEntry(String entryId, String fallbackTitle) {
        if (entryId == null || entryId.isBlank()) {
            return;
        }

        this.currentEntryId = entryId;
        exitEditModeUI();
        contentContainer.removeAll();

        VerticalLayout detailLayout = new VerticalLayout();
        detailLayout.setSizeFull();
        detailLayout.setPadding(true);
        Div header = createSectionHeader(resolveEntryTitle(entryId, fallbackTitle));
        detailLayout.add(header, createLoadingPlaceholder());
        contentContainer.add(detailLayout);

        final int requestId = detailLoadSequence.incrementAndGet();
        loadEntryDetail(entryId, entry -> {
            if (requestId != detailLoadSequence.get()) {
                return;
            }

            detailLayout.removeAll();
            EditorJs viewerEditor = new EditorJs(false);
            viewerEditor.setSizeFull();
            setupEditorContent(viewerEditor, entry.getContent());

            detailLayout.add(createSectionHeader(entry.getTitle()), viewerEditor);
            detailLayout.expand(viewerEditor);

            prefetchAdjacentEntries(entryId);
        }, error -> {
            if (requestId != detailLoadSequence.get()) {
                return;
            }
            log.error("Failed to load documentation detail for id={}", entryId, error);
            showErrorNotification(text("notification.loadError"), error);
        });
    }

    private String resolveEntryTitle(String entryId, String fallbackTitle) {
        if (fallbackTitle != null && !fallbackTitle.isBlank()) {
            return fallbackTitle;
        }
        return currentEntryIndexes.stream()
                .filter(entry -> entryId.equals(entry.getId()))
                .map(DocumentationEntryIndex::getTitle)
                .findFirst()
                .orElse("...");
    }

    private Div createLoadingPlaceholder() {
        Div loading = new Div(text("notification.loading"));
        loading.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Padding.Vertical.MEDIUM);
        return loading;
    }

    private void loadEntryDetail(String entryId, java.util.function.Consumer<DocumentationEntry> onSuccess, java.util.function.Consumer<Throwable> onError) {
        DocumentationEntry cached = detailCache.get(entryId);
        if (cached != null) {
            onSuccess.accept(cached);
            return;
        }

        final List<String> userRoles = resolveCurrentUserRoles();
        runAsync(() -> documentationService.getEntryDetailForRoles(entryId, userRoles), entry -> {
            if (entry == null) {
                onError.accept(new IllegalStateException("Documentation entry not found for id=" + entryId));
                return;
            }
            detailCache.put(entryId, entry);
            onSuccess.accept(entry);
        }, onError);
    }

    private void prefetchAdjacentEntries(String entryId) {
        int selectedIndex = -1;
        for (int i = 0; i < currentEntryIndexes.size(); i++) {
            if (entryId.equals(currentEntryIndexes.get(i).getId())) {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex < 0) {
            return;
        }

        for (int offset = 1; offset <= PREFETCH_NEIGHBORS; offset++) {
            prefetchEntryByPosition(selectedIndex - offset);
            prefetchEntryByPosition(selectedIndex + offset);
        }
    }

    private void prefetchEntryByPosition(int index) {
        if (index < 0 || index >= currentEntryIndexes.size()) {
            return;
        }

        String id = currentEntryIndexes.get(index).getId();
        if (id == null || detailCache.containsKey(id) || prefetchInProgress.contains(id)) {
            return;
        }

        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }

        prefetchInProgress.add(id);
        final List<String> userRoles = resolveCurrentUserRoles();
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return documentationService.getEntryDetailForRoles(id, userRoles);
                    } catch (Throwable t) {
                        throw new CompletionException(t);
                    }
                }, ioExecutor)
                .whenComplete((entry, error) -> {
                    if (ui.isClosing()) {
                        return;
                    }

                    ui.access(() -> {
                        prefetchInProgress.remove(id);
                        if (error != null || entry == null) {
                            return;
                        }
                        detailCache.put(id, entry);
                    });
                });
    }

    private List<String> resolveCurrentUserRoles() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return List.of();
            }
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
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

        runAsync(this::fetchEntriesForAdminEditScope, entries -> {
            if (!editMode) {
                return;
            }
            entries.forEach(this::addEntryEditor);
            addEntryButton.setIcon(VaadinIcon.PLUS.create());
            addEntryButton.setVisible(!isSingleEntryEditScope());
            contentContainer.add(entriesEditContainer, addEntryButton);
        }, error -> {
            log.error("Documentation edit mode initialization failed", error);
            showErrorNotification(text("notification.loadError"), error);
            exitEditModeUI();
        });
    }

    /**
     * Instantiates a new documentation entry and adds its editor to the container.
     */
    private void addNewEntry() {
        if (currentFilterType == null || currentFilterType.isBlank()) {
            showErrorNotification(text("notification.uploadError"), "Nejprve vyber typ dokumentace (kapitoly/modely/kvizy).");
            return;
        }
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
        if (currentEntryId != null) {
            showEntry(currentEntryId, null);
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
        List<CompletableFuture<DocumentationEntry>> futures = entryEditors.stream()
                .map(DocumentationEntryEditor::getEntry)
                .toList();

        CompletableFuture<List<DocumentationEntry>> collectEditedFuture = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());

        runFutureWithOverlay(collectEditedFuture, edited -> {
                    boolean hasInvalidType = edited.stream()
                            .anyMatch(entry -> entry.getType() == null || entry.getType().isBlank());
                    if (hasInvalidType) {
                        showErrorNotification(text("notification.uploadError"), "Každá položka musí mít vyplněný typ.");
                        return;
                    }

                    runAsyncVoid(() -> {
                    List<DocumentationEntry> allEntries = documentationService.getAllEntriesForSave();
                    Set<String> editedIds = edited.stream()
                            .map(DocumentationEntry::getId)
                            .filter(id -> id != null && !id.isBlank())
                            .collect(Collectors.toSet());

                    List<DocumentationEntry> merged = new ArrayList<>(allEntries.stream()
                            .filter(existing -> !editedIds.contains(existing.getId()))
                            .toList());
                    merged.addAll(edited);
                    documentationService.saveAll(merged);
                },
                () -> {
                    new SuccessNotification(text("doc.notification.saved"));
                    requestPageReload();
                },
                error -> {
                    log.error("Documentation save failed", error);
                    showErrorNotification(text("notification.uploadError"), error);
                });
                },
                error -> {
                    log.error("Collecting editor data for documentation save failed", error);
                    showErrorNotification(text("notification.uploadError"), error);
                });
    }

    private void requestPageReload() {
        getUI().ifPresent(ui -> {
            if (!ui.isClosing()) {
                ui.getPage().executeJs("window.location.reload()");
            }
        });
    }
}
