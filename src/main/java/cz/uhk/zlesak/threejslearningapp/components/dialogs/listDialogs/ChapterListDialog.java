package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ChapterSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;

/**
 * Dialog for listing and selecting ChapterEntity instances.
 * It extends the AbstractListDialog with ChapterEntity type.
 * Uses event-driven architecture to notify consumers when a chapter is selected.
 */
public class ChapterListDialog extends AbstractListDialog<ChapterEntity> {
    /**
     * Constructor for ChapterListDialog.
     * @param chapterListingView the ChapterListingView to be used in the dialog
     */
    public ChapterListDialog(ChapterListingView chapterListingView) {
        super(chapterListingView);
    }

    /**
     * Fires a ChapterSelectedFromDialogEvent when a chapter is selected.
     * This event is broadcast at the UI level with the selected chapter and blockId.
     * @param entity the selected chapter
     */
    @Override
    protected void fireEntitySelectedEvent(ChapterEntity entity) {
        ComponentUtil.fireEvent(
                UI.getCurrent(),
                new ChapterSelectedFromDialogEvent(UI.getCurrent(), false, entity, getBlockId())
        );
    }
}
