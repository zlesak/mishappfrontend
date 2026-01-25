package cz.uhk.zlesak.threejslearningapp.components.common;

import com.vaadin.flow.component.html.Hr;

/**
 * A styled divider component.
 * Extends the Vaadin Hr component with custom styles.
 */
public class Divider extends Hr {
    public Divider() {
        super();
        setWidth("80%");
        getStyle().set("margin", "0 auto");
    }
}
