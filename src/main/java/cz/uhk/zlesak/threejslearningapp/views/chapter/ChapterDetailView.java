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
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractChapterView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.yaml.snakeyaml.util.Tuple;

import java.util.List;
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
    public ChapterDetailView(ChapterService chapterService, ModelService modelService) {
        super("page.title.chapterView", chapterService, modelService);
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
        runAsync(() -> {
                    try {
                        String chapterName = chapterService.getChapterName(chapterId);
                        String chapterContent = chapterService.getChapterContent(chapterId);
                        Map<Triple<String, String, String>, List<Tuple<String, String>>> headers = chapterService.processHeaders(chapterId);
                        Map<String, QuickModelEntity> modelsMap = resolveFullModels(chapterService.getChaptersModels(chapterId));
                        return new ChapterDetailData(chapterName, chapterContent, headers, modelsMap);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                detailData -> {
                    nameTextField.setValue(detailData.chapterName());
                    editorjs.setChapterContentData(detailData.chapterContent());
                    ComponentUtil.fireEvent(UI.getCurrent(), new SubchapterInitEvent(UI.getCurrent(), detailData.headers(), false));
                    setupData(detailData.modelsMap());
                },
                error -> {
                    log.error("Error loading chapter data", error);
                    new ErrorNotification(text("error.modelLoadFailed") + ": " + error.getMessage());
                });
    }


    private record ChapterDetailData(
            String chapterName,
            String chapterContent,
            Map<Triple<String, String, String>, List<Tuple<String, String>>> headers,
            Map<String, QuickModelEntity> modelsMap
    ) {
    }
}
