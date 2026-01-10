package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Tag("div")
public class ModelListItem extends AbstractListItem {

    public ModelListItem(QuickModelEntity model, boolean listView, boolean administrationView) {
        super(listView, administrationView, VaadinIcon.CUBES);

        titleSpan.setText(model.getModel().getName());

        setOpenButtonClickListener(e -> {
            VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
            if (listView) {
                UI.getCurrent().navigate("model/" + model.getModel().getId());
            } else {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", "model/" + model.getId());
            }
        });

        setEditButtonClickListener(e -> {
            if (administrationView) {
                VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
                UI.getCurrent().navigate("createModel/" + model.getModel().getId());
            }
        });
    }
}
