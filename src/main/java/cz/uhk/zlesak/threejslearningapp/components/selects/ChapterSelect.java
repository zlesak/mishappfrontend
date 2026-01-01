package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubChapterChangeEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import org.springframework.context.annotation.Scope;

import java.util.List;

/**
 * ChapterSelect is a select box for selecting sub-chapters.
 * Fires UI-level SubChapterChangeEvent when selection changes.
 */
@Scope("prototype")
public class ChapterSelect extends GenericSelect<SubChapterForSelect> {
    /**
     * Constructor for ChapterSelect.
     * It initializes the select with an empty label, a text generator for items, and sets up the event handling for sub-chapter changes.
     * Calls the parent class constructor with the appropriate parameters.
     */
    public ChapterSelect() {
        super("", SubChapterForSelect::text, SubChapterForSelect.class, false, SubChapterForSelect::id);
        setEmptySelectionAllowed(true);
        setEmptySelectionCaption(text("chapterSelect.caption"));
        setWidthFull();
    }

    /**
     * This method is used to populate the select with sub-chapter records.
     * Calls the initialize method from the parent class to set the items.
     *
     * @param subChapters the list of sub-chapter records to be displayed in the select
     */
    public void initializeChapterSelectionSelect(List<SubChapterForSelect> subChapters) {
        initialize(subChapters);
    }

    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<SubChapterForSelect> event) {
        return new SubChapterChangeEvent(UI.getCurrent(), event.getOldValue(), event.getValue(), event.isFromClient());
    }

    @Override
    protected void handleFileUploadIngoingChangeEventAction(UploadFileEvent fileType) {

    }

    @Override
    protected void handleFileRemoveIngoingChangeEventAction(RemoveFileEvent fileType) {

    }

    @Override
    protected void handleIngoingActionChangeEventAction(ThreeJsActionEvent threeJsActionEvent) {

    }

    @Override
    protected void handleSubChapterChangeEventAction(SubChapterChangeEvent subChapterChangeEvent) {
        showRelevantItemsBasedOnContext(subChapterChangeEvent.getNewValue() != null ? subChapterChangeEvent.getNewValue().id() : "main", null, subChapterChangeEvent.isFromClient());
    }

    @Override
    protected void showRelevantItemsBasedOnContext(String entityId, SubChapterForSelect firstItemToSelectIfAvailable, boolean fromClient, String... specificEntityId) {
        if (items.containsKey(entityId)) {
            setValue(items.get(entityId));
        }
    }
}
