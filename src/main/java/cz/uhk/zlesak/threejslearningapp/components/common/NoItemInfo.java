package cz.uhk.zlesak.threejslearningapp.components.common;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

public class NoItemInfo extends Div implements I18nAware {

    public NoItemInfo(String infoTextKey) {
        super();
        getStyle().set("width", "100%");
        getStyle().set("display", "flex");
        getStyle().set("justify-content", "center");
        getStyle().set("align-items", "center");

        H1 header = new H1(text(infoTextKey));
        header.getStyle().set("font-size", "2em");
        header.getStyle().set("margin-top", "1em");
        header.getStyle().set("margin-bottom", "0.5em");
        header.getStyle().set("text-align", "center");
        add(header);
    }
}
