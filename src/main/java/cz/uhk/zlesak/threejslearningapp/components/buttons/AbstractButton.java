package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;


/**
 * Abstract button class that simplifies the creation of buttons with internationalized labels and click event handling.
 *
 * @param <T> the type of the component that will be the source of the click event
 */

public class AbstractButton<T extends Component> extends Button implements I18nAware {

    public AbstractButton(String labelKey, ComponentEvent<T> onClick, VaadinIcon icon, ButtonVariant... variants) {
        super();
        if (icon != null){
            setIcon(icon.create());
        }
        setText(text(labelKey));
        for (ButtonVariant variant : variants){
            addThemeVariants(variant);
        }
        if (onClick != null){
            addClickListener(e -> ComponentUtil.fireEvent(UI.getCurrent(), onClick));
        }
    }
}
