package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubchapterInitEvent;
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
@Tag("chapter-view")
@Scope("prototype")
@PermitAll
public class ChapterDetailView extends AbstractChapterView {
    private final ChapterService chapterService;

    private String chapterId;

    /**
     * ChapterView constructor - creates instance of chapter view instance that then accomplishes the goal of getting
     * and serving the user the requested chapter from proper backend API endpoint via chapterApiClient.
     * @param chapterService service for handling chapter-related operations
     */
    @Autowired
    public ChapterDetailView(ChapterService chapterService) {
        super("page.title.chapterDetailView" ,chapterService);
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

        try {
            ComponentUtil.fireEvent(UI.getCurrent(), new SubchapterInitEvent(UI.getCurrent(), chapterService.processHeaders(chapterId), false));
        } catch (Exception e) {
            log.error("Error loading chapter data", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads and displays all 3D models associated with the chapter.
     * For each model, loads the model file, textures, and sets up the renderer.
     *
     */
    private void loadAndDisplay3DModels() {
        try {
            Map<String, QuickModelEntity> modelsMap = chapterService.getChaptersModels(chapterId);
            setupData(modelsMap);
        } catch (Exception e) {
            log.error("Failed to load 3D models: {}", e.getMessage(), e);
            new ErrorNotification(text("error.modelLoadFailed") + ": " + e.getMessage(), 5000);
        }
    }
}
