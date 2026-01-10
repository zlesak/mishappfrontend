package cz.uhk.zlesak.threejslearningapp.components.inputs.textFields;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * A custom TextField component for search functionality.
 */
public class SearchTextField extends TextField implements I18nAware {
    public SearchTextField(String placeholder) {
        super();
        setValueChangeMode(ValueChangeMode.EAGER);
        setPlaceholder(text(placeholder));
    }
}
