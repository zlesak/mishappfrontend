package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;

/**
 * Dialog for listing and selecting ChapterEntity instances.
 * It extends the AbstractListDialog with ChapterEntity type.
 */
public class ChapterListDialog extends AbstractListDialog<ChapterEntity> {
    /**
     * Constructor for ChapterListDialog.
     * @param chapterListingView the ChapterListingView to be used in the dialog
     */
    public ChapterListDialog(ChapterListingView chapterListingView) {
        super(chapterListingView);
    }
}
