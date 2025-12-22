package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * A select component for choosing a QuickModelEntity.
 * The select is read-only and displays the name of the model.
 */
public class QuickModelSelect extends Select<QuickModelEntity> implements I18nAware {

    /**
     * Constructs a QuickModelSelect with the given label and id.
     *
     * @param label the label to display as helper text
     * @param id    the block id attribute to set on the element
     */
    public QuickModelSelect(String label, String id) {
        super();
        setHelperText(label);
        getElement().setAttribute("block-id", id);
        setItemLabelGenerator(entity -> entity != null ? entity.getModel().getName() : "");
        setWidthFull();
        setReadOnly(true);
    }
}
