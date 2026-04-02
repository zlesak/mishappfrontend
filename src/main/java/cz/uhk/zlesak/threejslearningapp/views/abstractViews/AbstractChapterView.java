package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.components.containers.ChapterTabSheetContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.SubchapterSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.NameTextField;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ChapterContentScroller;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ModelsSelectScroller;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractChapterView is an abstract base class for views related to chapter management, including creating, editing, and viewing chapters.
 * It provides a common layout and components such as navigation, content editor, and 3D model display.
 * The layout is responsive and adjusts based on the view type (create, edit, view).
 * It includes a secondary navigation bar with chapter selection and search functionality.
 * The class is designed to be extended by specific chapter-related views.
 */
@Slf4j
@Scope("prototype")
public abstract class AbstractChapterView extends AbstractEntityView<ChapterService> {
    private static final int DESKTOP_BREAKPOINT = 1024;
    protected final SearchTextField searchTextField = new SearchTextField("filter.search.placeholder");
    protected final SubchapterSelectContainer subchapterSelectContainer = new SubchapterSelectContainer();
    protected final EditorJs editorjs;
    protected final NameTextField nameTextField = new NameTextField("chapter.title");
    protected ChapterTabSheetContainer secondaryNavigation = null;
    private Button chapterNavigationToggleButton;
    private VerticalLayout chapterNavigationContent;
    private String chapterNavigationStateKey = "";
    private boolean chapterNavigationExpanded = true;
    private boolean compactChapterNavigationExpanded = true;
    private final ModelService modelService;
    private final Map<String, String> modelBackgroundSpecByModelId = new ConcurrentHashMap<>();

    /**
     * Constructor for AbstractChapterView in edit/view mode.
     *
     * @param pageTitleKey the title key for the page
     * @param service      the chapter service for handling chapter operations
     */
    public AbstractChapterView(String pageTitleKey, ChapterService service, ModelService modelService) {
        this(pageTitleKey, false, true, service, modelService);
    }

    /**
     * Constructor for AbstractChapterView.
     * Initializes the layout and components based on the specified view type.
     *
     * @param pageTitleKey          the title key for the page
     * @param createChapterMode     indicates if the view is in create chapter mode
     * @param skipBeforeLeaveDialog indicates if the before-leave dialog should be skipped
     * @param service               the chapter service for handling chapter operations
     */
    public AbstractChapterView(String pageTitleKey, boolean createChapterMode, boolean skipBeforeLeaveDialog, ChapterService service, ModelService modelService) {
        super(pageTitleKey, skipBeforeLeaveDialog, service);
        this.modelService = modelService;
        editorjs = new EditorJs(createChapterMode);
        ChapterContentScroller chapterContentScroller = new ChapterContentScroller(editorjs);
        ModelsSelectScroller modelsScroller = new ModelsSelectScroller();

        if (createChapterMode) {
            secondaryNavigation = new ChapterTabSheetContainer(nameTextField, chapterContentScroller, modelsScroller);
            Scroller tabsScroller = new Scroller(secondaryNavigation, Scroller.ScrollDirection.VERTICAL);
            tabsScroller.setSizeFull();
            entityContent.add(tabsScroller);
        } else {
            nameTextField.setWidthFull();
            HorizontalLayout horizontalLayout = new HorizontalLayout(nameTextField, searchTextField);
            horizontalLayout.setWidthFull();
            horizontalLayout.addClassName("chapter-nav-search-row");

            chapterNavigationToggleButton = new Button("Navigace kapitoly", VaadinIcon.ANGLE_DOWN.create());
            chapterNavigationToggleButton.addClassName("chapter-nav-toggle");
            chapterNavigationToggleButton.addClickListener(e -> setChapterNavigationExpanded(!chapterNavigationExpanded, true));

            chapterNavigationContent = new VerticalLayout(horizontalLayout, subchapterSelectContainer);
            chapterNavigationContent.addClassName("chapter-nav-content");
            chapterNavigationContent.setWidthFull();
            chapterNavigationContent.setPadding(false);
            chapterNavigationContent.setSpacing(true);

            entityContent.add(chapterNavigationToggleButton, chapterNavigationContent, chapterContentScroller);
        }

        searchTextField.addValueChangeListener(event -> editorjs.search(event.getValue()));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsActionEvent.class,
                event -> {
                    if (event.getAction() != ThreeJsActions.SHOW_MODEL) {
                        return;
                    }
                    applyBackgroundForModelId(event.getModelId());
                }
        ));

        if (chapterNavigationToggleButton != null && chapterNavigationContent != null) {
            attachEvent.getUI().getPage().executeJs("return window.location.pathname;")
                    .then(String.class, path -> {
                        chapterNavigationStateKey = "chapter.navigation." + path;
                        attachEvent.getUI().getPage()
                                .executeJs("return window.innerWidth;")
                                .then(Integer.class, width -> {
                                    int viewportWidth = width == null ? DESKTOP_BREAKPOINT : width;
                                    if (viewportWidth >= DESKTOP_BREAKPOINT) {
                                        applyChapterNavigationModeForWidth(viewportWidth);
                                        return;
                                    }

                                    attachEvent.getUI().getPage()
                                            .executeJs("const raw = sessionStorage.getItem($0); return raw === null ? '' : raw;", chapterNavigationStateKey)
                                            .then(String.class, stored -> {
                                                if (stored != null && !stored.isBlank()) {
                                                    compactChapterNavigationExpanded = Boolean.parseBoolean(stored);
                                                } else {
                                                    compactChapterNavigationExpanded = viewportWidth > 599;
                                                }
                                                applyChapterNavigationModeForWidth(viewportWidth);
                                            });
                                });
                    });
            registrations.add(attachEvent.getUI().getPage().addBrowserWindowResizeListener(
                    event -> applyChapterNavigationModeForWidth(event.getWidth())
            ));
        }
    }

    /**
     * Sets up the model div with event listeners, models and initializes texture selects.
     *
     * @param quickModelEntityMap a map of model IDs to QuickModelEntity objects used for initialization
     */
    protected void setupData(Map<String, QuickModelEntity> quickModelEntityMap) {
        if (quickModelEntityMap == null || quickModelEntityMap.isEmpty()) {
            return;
        }

        refreshModelBackgroundSpecs(quickModelEntityMap);
        loadModelsWithTextures(quickModelEntityMap);
        editorjs.initializeTextureSelects(quickModelEntityMap);
    }

    /**
     * Resolves lightweight chapter models to full entities using metadata IDs.
     * Intended for async/background usage before calling setupData() on UI thread.
     */
    protected Map<String, QuickModelEntity> resolveFullModels(Map<String, QuickModelEntity> quickModelEntityMap) {
        if (quickModelEntityMap == null || quickModelEntityMap.isEmpty()) {
            return Map.of();
        }

        Map<String, QuickModelEntity> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, QuickModelEntity> entry : quickModelEntityMap.entrySet()) {
            QuickModelEntity quickModelEntity = entry.getValue();
            if (quickModelEntity == null) {
                continue;
            }
            try {
                if (quickModelEntity.getMetadataId() != null && !quickModelEntity.getMetadataId().isBlank()) {
                    resolved.put(entry.getKey(), modelService.read(quickModelEntity.getMetadataId()));
                } else {
                    resolved.put(entry.getKey(), quickModelEntity);
                }
            } catch (Exception e) {
                log.error("Failed to resolve full model for key {}: {}", entry.getKey(), e.getMessage());
                resolved.put(entry.getKey(), quickModelEntity);
            }
        }
        return resolved;
    }

    /**
     * Loads multiple models along with their associated textures by firing UploadFileEvent events.
     *
     * @param quickModelEntityMap a map of model IDs to QuickModelEntity objects
     */
    protected void loadModelsWithTextures(Map<String, QuickModelEntity> quickModelEntityMap) {
        for (Map.Entry<String, QuickModelEntity> entry : quickModelEntityMap.entrySet()) {
            QuickModelEntity fullEntity = entry.getValue();
            if (fullEntity == null || fullEntity.getModel() == null || fullEntity.getModel().getId() == null) {
                continue;
            }
            loadSingleModelWithTextures(fullEntity, null, entry.getKey(), Objects.equals(entry.getKey(), "main"));
        }
    }

    /**
     * Stores the resolved background specification JSON for a given model entity,
     * so it can be applied when that model is shown in the 3D viewer.
     *
     * @param modelEntity the model entity whose background spec should be cached
     */
    protected void rememberModelBackgroundSpec(QuickModelEntity modelEntity) {
        if (modelEntity == null || modelEntity.getModel() == null || modelEntity.getModel().getId() == null) {
            return;
        }

        String modelId = modelEntity.getModel().getId();
        String backgroundSpecJson;
        if (modelEntity instanceof ModelEntity fullModelEntity) {
            backgroundSpecJson = modelService.resolveBackgroundSpecJson(fullModelEntity);
        } else {
            backgroundSpecJson = modelService.extractBackgroundSpecJson(modelEntity.getDescription());
        }
        if (backgroundSpecJson != null && !backgroundSpecJson.isBlank()) {
            modelBackgroundSpecByModelId.put(modelId, backgroundSpecJson);
        } else {
            modelBackgroundSpecByModelId.remove(modelId);
        }
    }

    private void refreshModelBackgroundSpecs(Map<String, QuickModelEntity> quickModelEntityMap) {
        modelBackgroundSpecByModelId.clear();
        for (QuickModelEntity entity : quickModelEntityMap.values()) {
            if (entity == null || entity.getModel() == null || entity.getModel().getId() == null) {
                continue;
            }
            try {
                rememberModelBackgroundSpec(entity);
            } catch (Exception e) {
                log.warn("AbstractChapterView: failed to resolve background for model {}: {}",
                        entity.getModel().getId(), e.getMessage());
            }
        }
    }

    private void applyBackgroundForModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return;
        }

        String backgroundSpecJson = modelBackgroundSpecByModelId.get(modelId);
        if (backgroundSpecJson == null || backgroundSpecJson.isBlank()) {
            log.info("AbstractChapterView: no background mapping for shown modelId={}, restoring default skybox", modelId);
            modelDiv.renderer.restoreDefaultBackground();
            return;
        }

        log.info("AbstractChapterView: applying mapped background for shown modelId={}", modelId);
        modelDiv.renderer.setBackgroundSpec(backgroundSpecJson);
    }

    /**
     * Configures the view to read-only mode.
     * Disables editing of the chapter name and content.
     */
    protected void configureReadOnlyMode() {
        nameTextField.setReadOnly(true);
    }

    private void setChapterNavigationExpanded(boolean expanded, boolean persist) {
        chapterNavigationExpanded = expanded;
        if (chapterNavigationContent != null) {
            chapterNavigationContent.setVisible(expanded);
        }
        if (chapterNavigationToggleButton != null) {
            chapterNavigationToggleButton.setIcon(expanded ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
            chapterNavigationToggleButton.setText(expanded ? "Navigace kapitoly (skrýt)" : "Navigace kapitoly (zobrazit)");
        }
        if (!persist || chapterNavigationStateKey == null || chapterNavigationStateKey.isBlank()) {
            return;
        }
        UI ui = UI.getCurrent();
        if (ui != null) {
            compactChapterNavigationExpanded = expanded;
            ui.getPage().executeJs("sessionStorage.setItem($0, $1);", chapterNavigationStateKey, String.valueOf(expanded));
        }
    }

    private void applyChapterNavigationModeForWidth(int viewportWidth) {
        boolean desktop = viewportWidth >= DESKTOP_BREAKPOINT;
        if (chapterNavigationToggleButton != null) {
            chapterNavigationToggleButton.setVisible(!desktop);
        }
        if (desktop) {
            setChapterNavigationExpanded(true, false);
        } else {
            setChapterNavigationExpanded(compactChapterNavigationExpanded, false);
        }
    }
}
