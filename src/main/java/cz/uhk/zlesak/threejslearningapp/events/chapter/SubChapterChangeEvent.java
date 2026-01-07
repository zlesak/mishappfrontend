package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import lombok.Getter;

/**
 * SubChapterChangeEvent represents an event triggered when a subchapter selection changes.
 * It extends ComponentEvent and includes information about the old and new subchapter values.
 */
@Getter
public class SubChapterChangeEvent extends ComponentEvent<Component> {
    private final SubChapterForSelect oldValue;
    private final SubChapterForSelect newValue;

    /**
     * Constructor for SubChapterChangeEvent.
     *
     * @param source     the component source of the event
     * @param oldValue   old subchapter value before the change
     * @param newValue   new subchapter value after the change
     * @param fromClient indicates if the event originated from the client side
     */
    public SubChapterChangeEvent(Component source, SubChapterForSelect oldValue, SubChapterForSelect newValue, boolean fromClient) {
        super(source, fromClient);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

}


