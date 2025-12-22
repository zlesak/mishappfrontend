package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.router.*;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubChapterChangeEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.TextureService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractChapterView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.Map;

/**
 * ChapterDetailView Class - Shows the requested chapter from URL parameter. Initializes all the necessary elements
 * to provide the user with the chapter content in intuitive way.
 */
@Slf4j
@Route("chapter/:chapterId?")
@JavaScript("./js/scroll-to-element-data-id.js")
@Tag("chapter-view")
@Scope("prototype")
@PermitAll
public class ChapterDetailView extends AbstractChapterView {
    private final ModelService modelService;
    private final TextureService textureService;
    private final ChapterService chapterService;

    private String chapterId;
    private Map<String, QuickModelEntity> modelsMap;

    /**
     * ChapterView constructor - creates instance of chapter view instance that then accomplishes the goal of getting
     * and serving the user the requested chapter from proper backend API endpoint via chapterApiClient.
     */
    @Autowired
    public ChapterDetailView(ChapterService chapterService, ModelService modelService, TextureService textureService) {
        super("page.title.chapterDetailView");
        this.chapterService = chapterService;
        this.modelService = modelService;
        this.textureService = textureService;
    }

    /**
     * Overridden beforeEnter function to check if the chapterId parameter is present in the URL.
     * If not, it redirects the user to the ChapterListView. If the chapterId is present, it stores it for later use.
     *
     * @param event before navigation event with event details
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        if (parameters.getParameterNames().isEmpty()) {
            event.forwardTo(ChapterListingView.class);
        }
        this.chapterId = event.getRouteParameters().get("chapterId").orElse(null);
        if (chapterId == null) {
            log.error("Nelze načíst kapitolu bez ID");
            new ErrorNotification("Nelze načíst kapitolu bez ID", 5000);
            UI.getCurrent().navigate(ChapterListingView.class);
        }
    }

    /**
     * Overridden afterNavigation function to initialize and load all the necessary data for the chapter view.
     *
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        configureReadOnlyMode();

        try {
            loadChapterData();
            loadAndDisplay3DModels();
        } catch (Exception e) {
            handleChapterLoadError(e);
        }
    }

    /**
     * Configures the view to read-only mode.
     * Disables editing of the chapter name and content.
     */
    private void configureReadOnlyMode() {
        nameTextField.setReadOnly(true);
        editorjs.toggleReadOnlyMode(true);
    }

    /**
     * Loads the main chapter data including name, content, and sub-chapters.
     *
     * @throws Exception if chapter data cannot be loaded
     */
    private void loadChapterData() throws Exception {
        nameTextField.setValue(chapterService.getChapterName(chapterId));
        editorjs.setChapterContentData(chapterService.getChapterContent(chapterId));
        chapterSelect.initializeChapterSelectionSelect(chapterService.getSubChaptersNames(chapterId));
        navigationContentLayout.initializeSubChapterData(chapterService.getSubChaptersContent(chapterId));
    }

    /**
     * Handles sub-chapter selection change.
     * Updates the displayed content and 3D model based on the selected sub-chapter.
     *
     * @param event the sub-chapter change event
     */
    private void handleSubChapterChange(SubChapterChangeEvent event) {
        try {
            SubChapterForSelect newValue = event.getNewValue();

            if (newValue == null) {
                showWholeChapter();
                return;
            }

            String subChapterId = newValue.id();
            editorjs.setSelectedSubchapterData(chapterService.getSelectedSubChapterContent(subChapterId));

            QuickModelEntity modelToShow = modelsMap.getOrDefault(subChapterId, modelsMap.get("main"));
            if (modelToShow != null) {
                modelDiv.modelTextureAreaSelectContainer.getModelListingSelect()
                        .setSelectedModelById(modelToShow.getModel().getId());
            }
        } catch (Exception e) {
            log.error("Error changing sub-chapter: {}", e.getMessage(), e);
            new ErrorNotification(text("error.subChapterLoadFailed") + ": " + e.getMessage(), 5000);
        }
    }

    /**
     * Shows the whole chapter content and selects the main model.
     */
    private void showWholeChapter() {
        editorjs.showWholeChapterData();
        if (modelsMap.containsKey("main")) {
            modelDiv.modelTextureAreaSelectContainer.getModelListingSelect()
                    .setSelectedModelById(modelsMap.get("main").getModel().getId());
        }
    }

    /**
     * Loads and displays all 3D models associated with the chapter.
     * For each model, loads the model file, textures, and sets up the renderer.
     *
     * @throws Exception if models cannot be loaded or displayed
     */
    private void loadAndDisplay3DModels() throws Exception {
        try {
            modelsMap = chapterService.getChaptersModels(chapterId);

            for (QuickModelEntity quickModelEntity : modelsMap.values()) {
                loadModelWithTextures(quickModelEntity);
            }

            setupData(modelsMap);
        } catch (Exception e) {
            log.error("Failed to load 3D models: {}", e.getMessage(), e);
            new ErrorNotification(text("error.modelLoadFailed") + ": " + e.getMessage(), 5000);
            throw e;
        }
    }

    /**
     * Loads a single model with its textures and adds it to the renderer.
     *
     * @param quickModelEntity the model entity to load
     * @throws Exception if model or textures cannot be loaded
     */
    private void loadModelWithTextures(QuickModelEntity quickModelEntity) throws Exception {
        String modelUrl = modelService.getModelFileBeEndpointUrl(quickModelEntity.getModel().getId());
        String textureUrl = null;

        if (quickModelEntity.getMainTexture() != null) {
            textureUrl = textureService.getTextureFileBeEndpointUrl(
                    quickModelEntity.getMainTexture().getTextureFileId()
            );
        }

        modelDiv.renderer.loadModel(modelUrl, textureUrl, quickModelEntity.getModel().getId());

        if (quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()) {
            Map<String, String> otherTexturesMap = TextureMapHelper.otherTexturesMap(
                    quickModelEntity.getOtherTextures(),
                    textureService
            );
            modelDiv.renderer.addOtherTextures(otherTexturesMap, quickModelEntity.getModel().getId());
        }
    }

    /**
     * Handles errors that occur during chapter loading.
     * Logs the error, shows a notification, and redirects to the chapter list.
     *
     * @param e the exception that occurred
     */
    private void handleChapterLoadError(Exception e) {
        log.error("Error loading chapter: {}", e.getMessage(), e);
        new ErrorNotification(text("error.chapterLoadFailed") + ": " + e.getMessage(), 5000);
        UI.getCurrent().navigate(ChapterListingView.class);
    }

    /**
     * On attach function to register event listeners when the view is attached.
     * Registers a listener for SubChapterChangeEvent to handle sub-chapter changes.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                SubChapterChangeEvent.class,
                event -> {
                    try {
                        handleSubChapterChange(event);
                    } catch (Exception e) {
                        log.error("Error changing sub-chapter: {}", e.getMessage(), e);
                        new ErrorNotification(text("error.subChapterLoadFailed") + ": " + e.getMessage(), 5000);
                    }
                }
        ));
    }
}
