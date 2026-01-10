package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * Event to show content of a specific subchapter.
 */
@Getter
public class ShowSubchapterContentEvent extends ComponentEvent<UI> {
    private final String subchapterId;
    private final String subchapterHeadingId;

    /**
     * Constructor for ShowSubchapterContentEvent.
     * @param source the source UI component
     * @param subchapterId subchapter identifier
     * @param subchapterHeadingId subchapter heading identifier
     * @param fromClient indicates if the event originated from the client side
     */
    public ShowSubchapterContentEvent(UI source, String subchapterId, String subchapterHeadingId, boolean fromClient) {
        super(source, fromClient);
        this.subchapterId = subchapterId;
        this.subchapterHeadingId = subchapterHeadingId;
    }
}
