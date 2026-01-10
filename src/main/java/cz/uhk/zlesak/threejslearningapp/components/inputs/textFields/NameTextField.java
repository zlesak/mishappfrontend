package cz.uhk.zlesak.threejslearningapp.components.inputs.textFields;

import com.vaadin.flow.component.textfield.TextField;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * A custom TextField component for chapter names.
 */
public class NameTextField extends TextField implements I18nAware {
    public NameTextField(String placeholder) {
        super();
        setPlaceholder(text(placeholder));
        setMaxLength(255);
        setRequired(true);
        setRequiredIndicatorVisible(true);
        getStyle().set("flex", "1 1 auto");
        getStyle().set("min-width", "0");
    }
}
