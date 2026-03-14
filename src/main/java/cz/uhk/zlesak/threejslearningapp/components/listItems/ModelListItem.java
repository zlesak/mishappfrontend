package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelCreateView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelDetailView;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag("div")
public class ModelListItem extends AbstractListItem {

    public ModelListItem(QuickModelEntity model, boolean listView, boolean administrationView) {
        super(listView, administrationView, VaadinIcon.CUBES);

        try {
            ModelService modelService = SpringContextUtils.getBean(ModelService.class);
            String desc = modelService.extractThumbnailDataUrl(model.getDescription());

            if (desc != null && !desc.isBlank()) {
                Image thumb = new Image(desc, model.getModel().getName());
                thumb.setWidthFull();
                addComponentAsFirst(thumb);
            }

        } catch (Exception ex) {
            log.warn("Failed to extract thumbnail from model description: {}", ex.getMessage());
        }

        titleSpan.setText(model.getModel().getName());
        details.removeAll();
        remove(details);

        setOpenButtonClickListener(e -> {
            VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
            if (listView) {
                UI.getCurrent().navigate(ModelDetailView.class, new RouteParameters(new RouteParam("modelId", model.getMetadataId())));
            } else {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", "model/" + model.getMetadataId());
            }
        });

        setEditButtonClickListener(e -> {
            if (administrationView) {
                VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
                UI.getCurrent().navigate(ModelCreateView.class, new RouteParameters(new RouteParam("modelId", model.getMetadataId())));
            }
        });

        setDeleteButtonClickListener(e -> {
            if (administrationView) {
                ConfirmDialog dialog = ConfirmDialog.createDeleteConfirmation(
                    "model",
                    model.getModel().getName(),
                    () -> deleteModel(model.getMetadataId())
                );
                dialog.open();
            }
        });
    }

    private void deleteModel(String modelId) {
        try {
            ModelService modelService = SpringContextUtils.getBean(ModelService.class);
            boolean deleted = modelService.delete(modelId);
            if (deleted) {
                new SuccessNotification(text("model.delete.success"));
                UI.getCurrent().getPage().reload();
            } else {
                new ErrorNotification(text("model.delete.failed"));
            }
        } catch (Exception ex) {
            log.error("Error deleting model: {}", ex.getMessage(), ex);
            new ErrorNotification(text("model.delete.error") + ": " + ex.getMessage());
        }
    }
}
