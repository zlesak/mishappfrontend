package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubChapterChangeEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
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
    private final ChapterService chapterService;

    private String chapterId;
    private Map<String, QuickModelEntity> modelsMap;

    /**
     * ChapterView constructor - creates instance of chapter view instance that then accomplishes the goal of getting
     * and serving the user the requested chapter from proper backend API endpoint via chapterApiClient.
     */
    @Autowired
    public ChapterDetailView(ChapterService chapterService) {
        super("page.title.chapterDetailView");
        configureReadOnlyMode();
        this.chapterService = chapterService;
    }

    /**
     * Overridden beforeEnter function to check if the chapterId parameter is present in the URL.
     * If not, it redirects the user to the ChapterListView. If the chapterId is present, it stores it for later use.
     *
     * @param event before navigation event with event details
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.chapterId = event.getRouteParameters().get("chapterId").orElse(null);
        if (this.chapterId == null) {
            event.forwardTo(ChapterListingView.class);
        }
    }

    /**
     * Overridden afterNavigation function to initialize and load all the necessary data for the chapter view.
     *
     * @param event after navigation event with event details
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        loadChapterData();
        loadAndDisplay3DModels();
    }


    /**
     * Loads the main chapter data including name, content, and sub-chapters.
     */
    private void loadChapterData() {
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
                editorjs.showWholeChapterData();
                if (modelsMap.containsKey("main")) {
                    modelDiv.modelTextureAreaSelectContainer.getModelListingSelect()
                            .setSelectedModelById(modelsMap.get("main").getModel().getId());
                }
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
     * Loads and displays all 3D models associated with the chapter.
     * For each model, loads the model file, textures, and sets up the renderer.
     *
     */
    private void loadAndDisplay3DModels() {
        try {
            modelsMap = chapterService.getChaptersModels(chapterId);
            setupData(modelsMap);
        } catch (Exception e) {
            log.error("Failed to load 3D models: {}", e.getMessage(), e);
            new ErrorNotification(text("error.modelLoadFailed") + ": " + e.getMessage(), 5000);
        }
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
                this::handleSubChapterChange
        ));
    }
}
