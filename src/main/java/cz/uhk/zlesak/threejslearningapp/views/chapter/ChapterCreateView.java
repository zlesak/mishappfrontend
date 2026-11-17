package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.components.forms.CreateChapterForm;
import cz.uhk.zlesak.threejslearningapp.common.LinkTextAudit;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.WarningNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.CreateChapterEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractChapterView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

import java.util.Map;

/**
 * ChapterCreateView for creating a new chapter or editing an existing one.
 * Extends AbstractChapterView and provides functionality to create/update chapters.
 */
@Slf4j
@Route("createChapter/:chapterId?")
@Tag("create-chapter")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class ChapterCreateView extends AbstractChapterView {
    private String chapterId;
    private boolean isEditMode = false;
    private final ModelService modelService;
    private CreateChapterForm createChapterForm;
    private volatile boolean editDataLoadingStarted = false;

    /**
     * Constructor for CreateChapterView.
     * Initializes the view with necessary services for creating chapters.
     *
     * @param chapterService service for handling chapter-related operations
     */
    @Autowired
    public ChapterCreateView(ChapterService chapterService, ModelService modelService) {
        super("page.title.createChapterView", true, false, chapterService, modelService);
        setCompactSplitterPosition(70);
        this.modelService = modelService;
        configureVisibility();
        setupChapterForm();
    }

    /**
     * Handles actions before entering the view.
     * Checks if chapterId is present for edit mode.
     *
     * @param event before navigation event
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.chapterId = event.getRouteParameters().get("chapterId").orElse(null);
        if (chapterId != null && !chapterId.isBlank()) {
            isEditMode = true;
            if (createChapterForm != null) {
                createChapterForm.getCreateChapterButton().setUpdateMode();
            }
        }
    }

    /**
     * Handles actions after navigation to the view.
     * Loads chapter data if in edit mode.
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (isEditMode && !editDataLoadingStarted) {
            editDataLoadingStarted = true;
            loadChapterForEdit();
        }
    }

    /**
     * Configures visibility of inherited components.
     */
    private void configureVisibility() {
        searchTextField.setVisible(false);
    }

    /**
     * Sets up the chapter creation form.
     */
    private void setupChapterForm() {
        createChapterForm = new CreateChapterForm(editorjs);
        entityContent.add(createChapterForm);
        secondaryNavigation.init(editorjs);
    }

    /**
     * Loads chapter data for editing.
     */
    private ChapterEntity loadedChapter;
    
    private void loadChapterForEdit() {
        final UI ui = UI.getCurrent();
        if (ui == null) {
            log.error("UI is not available for chapter edit loading");
            return;
        }

        ChapterEntity chapterFromSession = null;
        if (VaadinSession.getCurrent().getAttribute("chapterEntity") != null) {
            chapterFromSession = (ChapterEntity) VaadinSession.getCurrent().getAttribute("chapterEntity");
            VaadinSession.getCurrent().setAttribute("chapterEntity", null);
        }
        final ChapterEntity chapterFromSessionFinal = chapterFromSession;

        runAsync(() -> {
                    try {
                        ChapterEntity chapter = chapterFromSessionFinal != null ? chapterFromSessionFinal : service.read(chapterId);
                        String chapterContent = service.getChapterContent(chapterId);
                        Map<String, QuickModelEntity> fullModelsMap = resolveFullModels(service.getChaptersModels(chapterId));
                        return new ChapterEditData(chapter, chapterContent, fullModelsMap);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                editData -> {
                    loadedChapter = editData.chapter();
                    setChapterName(loadedChapter.getName());
                    editorjs.setChapterContentData(editData.chapterContent());

                    editorjs.getElement().executeJs(
                                    "return $0.editorReadyPromise",
                                    editorjs.getElement()
                            )
                            .toCompletableFuture().thenCompose(result -> editorjs.getSubchaptersNames())
                            .thenAccept(subchapterNames -> {
                                if (ui.isClosing()) {
                                    return;
                                }
                                ui.access(() -> {
                                    secondaryNavigation.getModelsScroller().initSelects(subchapterNames);

                                    if (editData.modelsMap() != null && !editData.modelsMap().isEmpty()) {
                                        editData.modelsMap().forEach((blockId, model) ->
                                                secondaryNavigation.getModelsScroller().updateModelSelect(blockId, model)
                                        );
                                    }
                                    setupData(editData.modelsMap());
                                });
                            })
                            .exceptionally(ex -> {
                                log.error("Editor initialization or model loading failed: {}", ex.getMessage(), ex);
                                if (!ui.isClosing()) {
                                    ui.access(() -> new ErrorNotification(text("error.editorInitializationFailed") + ": " + ex.getMessage()));
                                }
                                return null;
                            });
                },
                error -> {
                    log.error("Error loading chapter for edit: {}", error.getMessage(), error);
                    new ErrorNotification(text("error.chapterLoadFailed") + ": " + error.getMessage());
                    skipBeforeLeaveDialog = true;
                    UI.getCurrent().navigate(ChapterListingView.class);
                });
    }

    /**
     * Handles the CreateChapterEvent.
     * Retrieves data from the editor and creates a new chapter.
     * Upon successful creation, navigates to the newly created chapter view.
     *
     * @param event the create chapter event
     */
    private void createChapterConsumer(CreateChapterEvent event) {
        try {
            secondaryNavigation.setMainContentTabSelected();
            Map<String, QuickModelEntity> allModels = secondaryNavigation.getModelsScroller().getAllModelsMappedToChapterHeaderBlockId();
            retrieveEditorDataAndCreateChapter(allModels);
        } catch (ApplicationContextException ex) {
            log.error("Chapter creation error: {}", ex.getMessage(), ex);
            new ErrorNotification(text("error.chapterSaveFailed") + ": " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error during chapter creation: {}", ex.getMessage(), ex);
            new ErrorNotification(text("error.unexpectedChapterCreationError") + ": " + ex.getMessage());
        }
    }

    /**
     * Retrieves editor data and creates the chapter.
     *
     * @param allModels map of all selected models
     */
    private void retrieveEditorDataAndCreateChapter(Map<String, QuickModelEntity> allModels) {
        UI ui = UI.getCurrent();
        editorjs.getData()
                .thenAccept(bodyData -> {
                    if (ui == null || ui.isClosing()) {
                        log.warn("UI is not available for chapter save callback");
                        return;
                    }
                    ui.access(() -> createChapterAndNavigate(bodyData, allModels));
                })
                .exceptionally(error -> {
                    log.error("Error retrieving editor data: {}", error.getMessage(), error);
                    if (ui != null && !ui.isClosing()) {
                        ui.access(() -> new ErrorNotification(text("error.editorDataRetrievalFailed") + ": " + error.getMessage()));
                    }
                    return null;
                });
    }

    /**
     * Points out links whose text does not say where they go, at the moment the author saves.
     *
     * <p>Advice, not a barrier: the wording of the chapter is the author's, and refusing to save would
     * be the wrong way to make an editorial point. Links reading "ZDE" are content, so the only place
     * this can be raised is here.
     *
     * @param bodyData the chapter content about to be saved
     */
    private void warnAboutNonDescriptiveLinks(String bodyData) {
        var offenders = LinkTextAudit.nonDescriptiveLinkTexts(bodyData);
        if (offenders.isEmpty()) {
            return;
        }
        new WarningNotification(text("chapter.links.warning", String.join(", ", offenders)));
    }

    /**
     * Creates the chapter and navigates to it.
     *
     * @param bodyData  the chapter content
     * @param allModels map of all selected models
     */
    private void createChapterAndNavigate(String bodyData, Map<String, QuickModelEntity> allModels) {
        warnAboutNonDescriptiveLinks(bodyData);
        runAsync(() -> service.saveChapter(
                        chapterId,
                        isEditMode,
                        nameTextField.getValue(),
                        bodyData,
                        allModels,
                        loadedChapter
                ),
                savedChapterId -> {
                    if (isEditMode) {
                        new SuccessNotification(text("chapter.update.success"));
                    } else {
                        new SuccessNotification(text("chapter.create.success"));
                    }
                    skipBeforeLeaveDialog = true;
                    UI.getCurrent().navigate(ChapterDetailView.class, new RouteParameters(new RouteParam("chapterId", savedChapterId)));
                },
                error -> {
                    log.error("Error saving chapter: {}", error.getMessage(), error);
                    new ErrorNotification(text(isEditMode ? "error.chapterUpdateFailed" : "error.chapterCreationFailed") + ": " + error.getMessage());
                });
    }

    /**
     * Handles component attachment to the UI.
     * Registers a listener for CreateChapterEvent.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                CreateChapterEvent.class,
                this::createChapterConsumer
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelSelectedFromDialogEvent.class,
                event -> runAsync(() -> {
                            QuickModelEntity selectedModel = event.getSelectedModel();
                            QuickModelEntity fullModel = selectedModel;
                            if (selectedModel != null && selectedModel.getId() != null && !selectedModel.getId().isBlank()) {
                                fullModel = modelService.read(selectedModel.getId());
                            }
                            if (fullModel == null || fullModel.getModel() == null || fullModel.getModel().getId() == null) {
                                throw new ApplicationContextException(text("error.modelLoadFailed"));
                            }
                            return fullModel;
                        },
                        fullModel -> {
                            secondaryNavigation.getModelsScroller().updateModelSelect(
                                    event.getBlockId(),
                                    fullModel
                            );
                            rememberModelBackgroundSpec(fullModel);
                            loadSingleModelWithTextures(fullModel, event.getBlockId(), fullModel.getModel().getId(), true);
                            editorjs.initializeTextureSelects(secondaryNavigation.getModelsScroller().getAllModelsMappedToChapterHeaderBlockId());
                        },
                        error -> {
                            log.error("Failed to load full model data after model selection: {}", error.getMessage(), error);
                            new ErrorNotification(text("error.modelLoadFailed") + ": " + error.getMessage());
                        })
        ));
    }

    private record ChapterEditData(
            ChapterEntity chapter,
            String chapterContent,
            Map<String, QuickModelEntity> modelsMap
    ) {
    }
}
