package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Component for displaying a centered informational message when no items are available.
 */
public class NoItemInfoComponent extends Div implements I18nAware {

    /**
     * Constructs the component with the given i18n message key.
     *
     * @param infoTextKey i18n key for the displayed message
     */
    public NoItemInfoComponent(String infoTextKey) {
        super();
        getStyle().set("width", "100%");
        getStyle().set("display", "flex");
        getStyle().set("justify-content", "center");
        getStyle().set("align-items", "center");
        getStyle().set("grid-column", "1 / -1");

        H1 header = new H1(text(infoTextKey));
        header.getStyle().set("font-size", "2em");
        header.getStyle().set("margin-top", "1em");
        header.getStyle().set("margin-bottom", "0.5em");
        header.getStyle().set("text-align", "center");
        add(header);
    }
}
