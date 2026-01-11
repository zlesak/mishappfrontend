package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.components.buttons.ExistingModelSelectButton;
import cz.uhk.zlesak.threejslearningapp.components.selects.QuickModelSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;

@Getter
public class ModelSelectContainer extends HorizontalLayout implements I18nAware {
    private final Select<QuickModelEntity> select;

    public ModelSelectContainer(String label, String id, boolean main) {
        super();
        setWidthFull();
        select = new QuickModelSelect(label, id);
        ExistingModelSelectButton alreadyCreatedModelButton = new ExistingModelSelectButton(
                text("modelSelectButton.label"),
                select
        );
        add(select, alreadyCreatedModelButton);

        if (!main) {
            setId("select-models-tab-piece-" + id);
        }
    }

}

