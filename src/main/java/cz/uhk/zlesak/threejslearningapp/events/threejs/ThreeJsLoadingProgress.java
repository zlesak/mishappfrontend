package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import lombok.Getter;

/**
 * Event representing the progress of loading resources in a Three.js component.
 * This event is fired when there is an update in the loading progress of resources such as models or textures.
 * It contains information about the percentage of loading
 */
@Getter
public class ThreeJsLoadingProgress extends ComponentEvent<ThreeJsComponent> {
    private final int percent;
    private final String description;

    /**
     * Constructor for ThreeJsLoadingProgress event.
     * @param source the ThreeJsComponent that is the source of the event
     * @param percent the percentage of loading progress (0-100)
     * @param description a description of the current loading action or status
     */
    public ThreeJsLoadingProgress(ThreeJsComponent source, int percent, String description) {
        super(source, false);
        this.percent = percent;
        this.description = description;
    }
}

