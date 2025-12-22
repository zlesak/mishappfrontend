package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.orderedlayout.Scroller;
import cz.uhk.zlesak.threejslearningapp.components.containers.ChapterNavigationContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.ChapterTabSheetContainer;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.components.editors.MarkdownEditor;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.NameTextField;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ChapterContentScroller;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ModelsSelectScroller;
import cz.uhk.zlesak.threejslearningapp.components.selects.ChapterSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
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
@JavaScript("./js/scroll-to-element-data-id.js")
@Scope("prototype")
public abstract class AbstractChapterView extends AbstractEntityView {
    protected final SearchTextField searchTextField = new SearchTextField("Hledat v kapitole");
    protected final ChapterSelect chapterSelect = new ChapterSelect();
    protected final ChapterNavigationContainer navigationContentLayout = new ChapterNavigationContainer(chapterSelect, searchTextField);
    protected final MarkdownEditor mdEditor = new MarkdownEditor();
    protected final EditorJs editorjs = new EditorJs();
    protected final NameTextField nameTextField = new NameTextField("Název kapitoly");
    protected ChapterTabSheetContainer secondaryNavigation = null;



    /**
     * Constructor for AbstractChapterView.
     * Initializes the layout and components based on the specified view type.
     *
     */
    public AbstractChapterView(String pageTitleKey) {
        this(pageTitleKey, false, true);
    }
    public AbstractChapterView(String pageTitleKey, boolean createChapterMode, boolean skipBeforeLeaveDialog) {
        super(pageTitleKey, skipBeforeLeaveDialog);
        ChapterContentScroller chapterContentScroller = new ChapterContentScroller(editorjs, mdEditor);
        ModelsSelectScroller modelsScroller = new ModelsSelectScroller();

        if (createChapterMode) {
            secondaryNavigation = new ChapterTabSheetContainer(nameTextField, chapterContentScroller, modelsScroller);
            Scroller tabsScroller = new Scroller(secondaryNavigation, Scroller.ScrollDirection.VERTICAL);
            tabsScroller.setSizeFull();
            entityContent.add(tabsScroller);
        } else {
            entityContentNavigation.setVisible(true);
            entityContentNavigation.add(navigationContentLayout);
            nameTextField.setWidthFull();
            entityContent.add(nameTextField, chapterContentScroller);
        }

        searchTextField.addValueChangeListener(
                event -> editorjs.search(event.getValue())
        );
    }

    /**
     * Sets up the model div with event listeners and initializes texture selects.
     *
     * @param quickModelEntityMap a map of model IDs to QuickModelEntity objects used for initialization
     */
    protected void setupData(Map<String, QuickModelEntity> quickModelEntityMap) {
        editorjs.addModelTextureColorAreaClickListener((modelId, textureId, hexColor, text) -> {
            modelDiv.modelTextureAreaSelectContainer.getModelListingSelect().setSelectedModelById(modelId);
            modelDiv.modelTextureAreaSelectContainer.getTextureListingSelect().setSelectedTextureById(textureId);
            modelDiv.modelTextureAreaSelectContainer.getTextureAreaSelect().setSelectedAreaByHexColor(hexColor, textureId);
        });
        editorjs.initializeTextureSelects(quickModelEntityMap);
       setupModelDiv(quickModelEntityMap);
    }
}
