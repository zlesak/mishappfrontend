package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * Event fired when a new chapter is to be created.
 */
@Getter
public class CreateChapterEvent extends ComponentEvent<UI> {
    public CreateChapterEvent(UI source) {
        super(source, true);
    }
}
