package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import cz.uhk.zlesak.threejslearningapp.components.containers.ChapterTabSheetContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.SubchapterSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.NameTextField;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ChapterContentScroller;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ModelsSelectScroller;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.Map;

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
    protected final SearchTextField searchTextField = new SearchTextField("filter.search.placeholder");
    protected final SubchapterSelectContainer subchapterSelectContainer = new SubchapterSelectContainer();
    protected final EditorJs editorjs;
    protected final NameTextField nameTextField = new NameTextField("chapter.title");
    protected ChapterTabSheetContainer secondaryNavigation = null;
    private final boolean createMode;

    /**
     * Constructor for AbstractChapterView in edit/view mode.
     *
     * @param pageTitleKey the title key for the page
     * @param service      the chapter service for handling chapter operations
     */
    public AbstractChapterView(String pageTitleKey, ChapterService service) {
        this(pageTitleKey, false, true, service);
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
    public AbstractChapterView(String pageTitleKey, boolean createChapterMode, boolean skipBeforeLeaveDialog, ChapterService service) {
        super(pageTitleKey, skipBeforeLeaveDialog, service);
        editorjs = new EditorJs(createChapterMode);
        ChapterContentScroller chapterContentScroller = new ChapterContentScroller(editorjs);
        ModelsSelectScroller modelsScroller = new ModelsSelectScroller();
        this.createMode = createChapterMode;

        if (createChapterMode) {
            secondaryNavigation = new ChapterTabSheetContainer(nameTextField, chapterContentScroller, modelsScroller);
            Scroller tabsScroller = new Scroller(secondaryNavigation, Scroller.ScrollDirection.VERTICAL);
            tabsScroller.setSizeFull();
            entityContent.add(tabsScroller);
        } else {
            nameTextField.setWidthFull();
            HorizontalLayout horizontalLayout = new HorizontalLayout(nameTextField, searchTextField);
            horizontalLayout.setWidthFull();
            entityContent.add(horizontalLayout, subchapterSelectContainer, chapterContentScroller);
        }

        searchTextField.addValueChangeListener(event -> editorjs.search(event.getValue()));
    }

    /**
     * Sets up the model div with event listeners, models and initializes texture selects.
     *
     * @param quickModelEntityMap a map of model IDs to QuickModelEntity objects used for initialization
     */
    protected void setupData(Map<String, QuickModelEntity> quickModelEntityMap) {
        loadModelsWithTextures(quickModelEntityMap);
        if (createMode) {
            editorjs.initializeTextureSelects(secondaryNavigation.getModelsScroller().getAllModelsMappedToChapterHeaderBlockId());
        }
    }

    /**
     * Configures the view to read-only mode.
     * Disables editing of the chapter name and content.
     */
    protected void configureReadOnlyMode() {
        nameTextField.setReadOnly(true);
    }
}
