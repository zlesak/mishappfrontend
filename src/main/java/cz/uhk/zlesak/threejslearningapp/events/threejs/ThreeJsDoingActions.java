package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import lombok.Getter;

@Getter
public class ThreeJsDoingActions extends ComponentEvent<ThreeJsComponent> {
    private final String description;

    public ThreeJsDoingActions(ThreeJsComponent source, String description) {
        super(source, false);
        this.description = description;
    }
}
