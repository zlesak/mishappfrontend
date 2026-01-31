package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;

public class ThreeJsFinishedActions  extends ComponentEvent<ThreeJsComponent> {
    public ThreeJsFinishedActions(ThreeJsComponent source) {
        super(source, false);
    }
}
