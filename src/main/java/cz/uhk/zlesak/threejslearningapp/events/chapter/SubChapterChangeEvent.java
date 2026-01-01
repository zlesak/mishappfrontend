package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import lombok.Getter;

/**
 * Event fired when the selected sub-chapter in the ChapterSelect changes.
 * This event carries the old and new values of the selected sub-chapter to allow listeners to react accordingly.
 * This event is broadcast at the UI level to decouple components.
 */
@Getter
public class SubChapterChangeEvent extends ComponentEvent<UI> {
    private final SubChapterForSelect oldValue;
    private final SubChapterForSelect newValue;

    public SubChapterChangeEvent(UI source, SubChapterForSelect oldValue, SubChapterForSelect newValue, boolean fromClient) {
        super(source, fromClient);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

}


