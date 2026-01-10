package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;

/**
 * Event to scroll to a specific element within the EditorJs content.
 */
@Getter
public class ScrollToElement extends ComponentEvent<UI> {
    private final String element;

    /**
     * Constructor for ScrollToElement event.
     * @param source the source UI component
     * @param element the element identifier to scroll to
     * @param fromClient indicates if the event originated from the client side
     */
    public ScrollToElement(UI source, String element, boolean fromClient) {
        super(source, fromClient);
        this.element = element;
    }
}
