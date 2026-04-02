package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import lombok.Getter;

/**
 * Event fired when a Three.js component starts processing an action.
 */
@Getter
public class ThreeJsDoingActions extends ComponentEvent<ThreeJsComponent> {
    private final String description;

    /**
     * @param source      The Three.js component firing the event.
     * @param description Human-readable description of the ongoing action.
     */
    public ThreeJsDoingActions(ThreeJsComponent source, String description) {
        super(source, false);
        this.description = description;
    }
}
