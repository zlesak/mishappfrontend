package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.components.buttons.ExistingModelSelectButton;
import cz.uhk.zlesak.threejslearningapp.components.inputs.selects.QuickModelSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;

/**
 * Container combining a model select dropdown and an existing-model button for a single block.
 */
@Getter
public class ModelSelectContainer extends HorizontalLayout implements I18nAware {
    private final Select<QuickModelEntity> select;
    final String componentId;

    /**
     * Constructs a ModelSelectContainer.
     * @param label label for the select component
     * @param id   block ID attribute
     * @param main indicates if this is for the main chapter
     * @param showInfo whether to show the select component
     */
    public ModelSelectContainer(String label, String id, boolean main, boolean showInfo) {
        super();
        componentId = id;
        setWidthFull();
        select = new QuickModelSelect(label, id);
        ExistingModelSelectButton alreadyCreatedModelButton = new ExistingModelSelectButton(
                text("modelSelectButton.label"),
                id
        );
        add(select, alreadyCreatedModelButton);

        select.setVisible(showInfo);

        if (!main) {
            setId("select-models-tab-piece-" + id);
        }
    }

}

