package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;

/**
 * Event fired when a Three.js component finishes processing all actions.
 */
public class ThreeJsFinishedActions  extends ComponentEvent<ThreeJsComponent> {
    /**
     * @param source The Three.js component firing the event.
     */
    public ThreeJsFinishedActions(ThreeJsComponent source) {
        super(source, false);
    }
}
