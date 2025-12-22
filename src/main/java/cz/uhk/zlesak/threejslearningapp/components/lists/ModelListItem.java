package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
@Tag("div")
public class ModelListItem extends AbstractListItem {

    public ModelListItem(QuickModelEntity model, boolean listView) {
        super(listView);


        HorizontalLayout modelName = new HorizontalLayout();
        Span nameLabel = new Span(text("model.title") + ": ");
        Span name = new Span(model.getModel().getName());
        name.getStyle().set("font-weight", "600");
        modelName.add(nameLabel, name);
        details.add(modelName);

        if (model.getMainTexture() != null) {
            HorizontalLayout creatorRow = new HorizontalLayout();
            creatorRow.add(new Span(text("model.mainTexture") + ": "), new Span(model.getMainTexture().getName()));
            details.add(creatorRow);
        }
        if (model.getOtherTextures() != null && !model.getOtherTextures().isEmpty()) {
            HorizontalLayout modelsRow = new HorizontalLayout();
            modelsRow.setAlignItems(HorizontalLayout.Alignment.CENTER);
            modelsRow.add(new Span(text("model.otherTextures") + ": "));
            List<String> otherTexturesNames = model.getOtherTextures().stream()
                    .filter(texture -> texture != null && texture.getName() != null)
                    .map(QuickTextureEntity::getName)
                    .toList();
            modelsRow.add(new Span(String.join(", ", otherTexturesNames)));
            details.add(modelsRow);
        }

        setOpenButtonClickListener(e -> {
            VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
            if (listView) {
                UI.getCurrent().navigate("model/" + model.getModel().getId());
            } else {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", "model/" + model.getId());
            }
        });
    }
}
