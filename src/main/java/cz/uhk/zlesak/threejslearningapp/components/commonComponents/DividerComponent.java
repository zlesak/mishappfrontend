package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.html.Hr;

/**
 * A styled divider component.
 * Extends the Vaadin Hr component with custom styles.
 */
public class DividerComponent extends Hr {
    public DividerComponent() {
        super();
        setWidth("80%");
        getStyle().set("margin", "0 auto");
    }
}
