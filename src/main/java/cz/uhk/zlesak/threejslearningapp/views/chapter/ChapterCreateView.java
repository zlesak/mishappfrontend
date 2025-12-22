package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.components.forms.CreateChapterForm;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.CreateChapterEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.TextureService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractChapterView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChapterCreateView for creating a new chapter.
 * Accessible at the route "/createChapter".
 */
@Slf4j
@Route("createChapter")
@Tag("create-chapter")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class ChapterCreateView extends AbstractChapterView {
    private final TextureService textureService;
    private final ModelService modelService;
    private final ChapterService chapterService;

    /**
     * Constructor for CreateChapterView.
     * Initializes the view with necessary services for creating chapters.
     *
     * @param chapterService service for handling chapter-related operations
     * @param modelService   service for handling model-related operations
     * @param textureService service for handling texture-related operations
     */
    @Autowired
    public ChapterCreateView(ChapterService chapterService, ModelService modelService, TextureService textureService) {
        super("page.title.createChapterView", true, false);

        this.modelService = modelService;
        this.textureService = textureService;
        this.chapterService = chapterService;

        initializeView();
    }

    /**
     * Initializes the view components and sets up event listeners.
     */
    private void initializeView() {
        configureVisibility();
        setupChapterForm();
        setupModelSelectionHandler();
    }

    /**
     * Configures visibility of inherited components.
     */
    private void configureVisibility() {
        chapterSelect.setVisible(false);
        searchTextField.setVisible(false);
        navigationContentLayout.setVisible(false);
        editorjs.toggleReadOnlyMode(false);
    }

    /**
     * Sets up the chapter creation form.
     */
    private void setupChapterForm() {
        CreateChapterForm createChapterForm = new CreateChapterForm(editorjs, mdEditor);
        entityContent.add(createChapterForm);
        secondaryNavigation.init(editorjs);
    }

    /**
     * Sets up the model selection handler.
     * When a model is selected, loads it into the renderer.
     */
    private void setupModelSelectionHandler() {
        secondaryNavigation.getModelsScroller().setModelSelectedConsumer(quickModelEntity -> {
            try {
                loadModelsIntoRenderer(quickModelEntity);
            } catch (IOException e) {
                handleModelLoadError(e);
            }
        });
    }

    /**
     * Loads models into the renderer and prepares editor texture area selects.
     *
     * @param quickModelEntityMap map of model entities to be loaded
     * @throws IOException if there is an error loading model or texture streams
     */
    private void loadModelsIntoRenderer(Map<String, QuickModelEntity> quickModelEntityMap) throws IOException {
        for (QuickModelEntity quickModelEntity : quickModelEntityMap.values()) {
            loadSingleModelWithTextures(quickModelEntity);
        }
        setupData(quickModelEntityMap);
    }

    /**
     * Loads a single model with its textures into the renderer.
     *
     * @param quickModelEntity the model entity to load
     * @throws IOException if there is an error loading model or texture streams
     */
    private void loadSingleModelWithTextures(QuickModelEntity quickModelEntity) throws IOException {
        String modelUrl = modelService.getModelFileBeEndpointUrl(
                quickModelEntity.getModel().getId()
        );

        String textureUrl = getMainTextureUrl(quickModelEntity);
        modelDiv.renderer.loadModel(modelUrl, textureUrl, quickModelEntity.getModel().getId());

        if (textureUrl != null) {
            loadOtherTextures(quickModelEntity);
        }
    }

    /**
     * Gets the main texture URL for a model.
     *
     * @param quickModelEntity the model entity
     * @return the texture URL or null if no texture
     */
    private String getMainTextureUrl(QuickModelEntity quickModelEntity) {
        if (quickModelEntity.getMainTexture() != null) {
            return textureService.getTextureFileBeEndpointUrl(
                    quickModelEntity.getMainTexture().getTextureFileId()
            );
        }
        return null;
    }

    /**
     * Loads additional textures for a model.
     *
     * @param quickModelEntity the model entity
     * @throws IOException if there is an error loading textures
     */
    private void loadOtherTextures(QuickModelEntity quickModelEntity) throws IOException {
        List<QuickTextureEntity> allTextures = new ArrayList<>(quickModelEntity.getOtherTextures());
        Map<String, String> texturesMap = TextureMapHelper.otherTexturesMap(allTextures, textureService);
        modelDiv.renderer.addOtherTextures(texturesMap, quickModelEntity.getModel().getId());
    }

    /**
     * Handles errors that occur during model loading.
     *
     * @param e the IOException that occurred
     */
    private void handleModelLoadError(IOException e) {
        log.error("Error loading model for renderer: {}", e.getMessage(), e);
        new ErrorNotification(text("error.modelLoadFailed") + ": " + e.getMessage(), 5000);
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
            prepareForChapterCreation();
            Map<String, QuickModelEntity> allModels = getAllSelectedModels();
            retrieveEditorDataAndCreateChapter(allModels);
        } catch (ApplicationContextException ex) {
            handleChapterCreationError(ex);
        } catch (Exception ex) {
            handleUnexpectedError(ex);
        }
    }

    /**
     * Prepares the view for chapter creation.
     */
    private void prepareForChapterCreation() {
        secondaryNavigation.setMainContentTabSelected();
    }

    /**
     * Gets all models selected for the chapter.
     *
     * @return map of all selected models
     */
    private Map<String, QuickModelEntity> getAllSelectedModels() {
        return secondaryNavigation.getModelsScroller().getAllModelsMappedToChapterHeaderBlockId();
    }

    /**
     * Retrieves editor data and creates the chapter.
     *
     * @param allModels map of all selected models
     */
    private void retrieveEditorDataAndCreateChapter(Map<String, QuickModelEntity> allModels) {
        editorjs.getData()
                .whenComplete((bodyData, error) -> handleEditorDataResult(bodyData, error, allModels))
                .exceptionally(this::handleEditorDataException);
    }

    /**
     * Handles the result of editor data retrieval.
     *
     * @param bodyData  the editor body data
     * @param error     any error that occurred
     * @param allModels map of all selected models
     */
    private void handleEditorDataResult(String bodyData, Throwable error, Map<String, QuickModelEntity> allModels) {
        if (error != null) {
            log.error("Error retrieving editor data: {}", error.getMessage(), error);
            throw new ApplicationContextException(text("error.editorDataRetrievalFailed") + ": " + error.getMessage());
        }

        createChapterAndNavigate(bodyData, allModels);
    }

    /**
     * Creates the chapter and navigates to it.
     *
     * @param bodyData  the chapter content
     * @param allModels map of all selected models
     */
    private void createChapterAndNavigate(String bodyData, Map<String, QuickModelEntity> allModels) {
        try {
            String chapterName = nameTextField.getValue().trim();
            String chapterId = chapterService.create(
                    ChapterEntity.builder()
                            .name(chapterName)
                            .modelHeaderMap(allModels)
                            .content(bodyData)
                            .models(allModels.values().stream().toList())
                            .build()).getId();

            skipBeforeLeaveDialog = true;
            UI.getCurrent().navigate("chapter/" + chapterId);
        } catch (Exception e) {
            log.error("Error creating chapter: {}", e.getMessage(), e);
            throw new ApplicationContextException(text("error.chapterCreationFailed") + ": " + e.getMessage(), e);
        }
    }

    /**
     * Handles exceptions during editor data retrieval.
     *
     * @param error the error that occurred
     * @return null (required by exceptionally - returns type matches CompletableFuture generic)
     */
    private String handleEditorDataException(Throwable error) {
        if (error.getCause() instanceof ApplicationContextException) {
            new ErrorNotification(error.getCause().getMessage(), 5000);
        } else {
            log.error("Unexpected error: {}", error.getMessage(), error);
            new ErrorNotification(text("error.unexpectedError") + ": " + error.getMessage(), 5000);
        }
        return null;
    }

    /**
     * Handles chapter creation errors.
     *
     * @param ex the exception
     */
    private void handleChapterCreationError(ApplicationContextException ex) {
        log.error("Chapter creation error: {}", ex.getMessage(), ex);
        new ErrorNotification(text("error.chapterSaveFailed") + ": " + ex.getMessage(), 5000);
    }

    /**
     * Handles unexpected errors.
     *
     * @param ex the exception
     */
    private void handleUnexpectedError(Exception ex) {
        log.error("Unexpected error during chapter creation: {}", ex.getMessage(), ex);
        new ErrorNotification(text("error.unexpectedChapterCreationError") + ": " + ex.getMessage(), 5000);
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
    }
}

