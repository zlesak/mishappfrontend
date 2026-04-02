package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.NameTextField;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ChapterContentScroller;
import cz.uhk.zlesak.threejslearningapp.components.scrollers.ModelsSelectScroller;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A TabSheet component for secondary navigation within a chapter.
 * It contains tabs for chapter content and 3D model selection.
 * Allows switching between content and model views.
 *
 */
@Slf4j
public class ChapterTabSheetContainer extends TabSheet implements I18nAware {

    @Getter
    private final ModelsSelectScroller modelsScroller;
    private final ChapterContentScroller chapterContentScroller;
    private final Tab tabModels;

    public ChapterTabSheetContainer(NameTextField nameTextField, ChapterContentScroller chapterContentScroller, ModelsSelectScroller modelsScroller) {
        super();
        this.tabModels = new Tab(VaadinIcon.CHART_3D.create(), new Span(text("tab.models.label")));
        this.chapterContentScroller = chapterContentScroller;
        this.modelsScroller = modelsScroller;
        Tab tabContent = new Tab(VaadinIcon.FILE_TEXT.create(), new Span(text("tab.content.label")));
        add(tabContent, chapterContentScroller);
        add(tabModels, modelsScroller);
        addClassName("chapter-secondary-tabs");
        setPrefixComponent(nameTextField);
        setSizeFull();

        setSelectedIndex(0);
    }

    /**
     * Sets the main content tab as selected.
     * Important for resetting the view to the content tab.
     */
    public void setMainContentTabSelected() {
        setSelectedIndex(0);
    }

    /**
     * Initializes the component with the given EditorJsComponent.
     * @param editorJs the EditorJsComponent to retrieve subchapter names from
     */
    public void init(EditorJs editorJs) {
        addSelectedChangeListener(event -> {
            if(event.getSelectedTab() == tabModels) {
                chapterContentScroller.setEnabled(true); //DO NOT REMOVE!! - could not get editorjs content normally when scroller is disabled

                editorJs.getSubchaptersNames().whenComplete((subchapterNames, exception) -> {
                    if(exception != null) {
                        log.error("Error while getting subchapter names for model selects initialization", exception);
                        return;
                    }
                    modelsScroller.initSelects(subchapterNames);
                    chapterContentScroller.setEnabled(false);
                });
            }
        });
    }
}
