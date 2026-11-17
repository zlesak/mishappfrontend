package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.QuickChapterEntity;
import lombok.Getter;

/**
 * Event fired when a chapter is selected from the ChapterListDialog.
 */
@Getter
public class ChapterSelectedFromDialogEvent extends ComponentEvent<UI> {
    private final QuickChapterEntity selectedChapter;
    private final String blockId;

    /**
     * Creates a new ChapterSelectedFromDialogEvent.
     *
     * @param source          The UI component that fires the event
     * @param fromClient      Whether the event originated from the client side
     * @param selectedChapter The chapter that was selected
     * @param blockId         The block ID (if applicable)
     */
    public ChapterSelectedFromDialogEvent(UI source, boolean fromClient, QuickChapterEntity selectedChapter, String blockId) {
        super(source, fromClient);
        this.selectedChapter = selectedChapter;
        this.blockId = blockId;
    }
}

