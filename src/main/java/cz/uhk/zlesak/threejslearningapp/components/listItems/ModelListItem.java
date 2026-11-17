package cz.uhk.zlesak.threejslearningapp.components.listItems;

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

/**
 * List item component representing a 3D model in listing and administration views.
 * Displays an optional thumbnail image extracted from the model description.
 */
@Slf4j
public class ModelListItem extends EntityRow {

    /** Big enough to recognise the preparation, small enough to keep the row one line tall. */
    private static final String THUMBNAIL_SIZE = "56px";

    /**
     * Constructs the model list item.
     *
     * @param model              the model entity to display
     * @param listView           whether to render in list (read) mode
     * @param administrationView whether to show edit and delete controls
     */
    public ModelListItem(QuickModelEntity model, boolean listView, boolean administrationView) {
        super(listView, administrationView, VaadinIcon.CUBES);

        setRowTitle(model.getModel().getName());
        addCommonMetadata(model);
        addThumbnail(model);

        setOpenButtonClickListener(e -> {
            VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
            if (listView) {
                UI.getCurrent().navigate(ModelDetailView.class, new RouteParameters(new RouteParam("modelId", model.getId())));
            } else {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", "model/" + model.getId());
            }
        });

        setEditButtonClickListener(e -> {
            if (administrationView) {
                VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
                UI.getCurrent().navigate(ModelCreateView.class, new RouteParameters(new RouteParam("modelId", model.getId())));
            }
        });

        setDeleteButtonClickListener(e -> {
            if (administrationView) {
                ConfirmDialog dialog = ConfirmDialog.createDeleteConfirmation(
                    "model",
                    model.getModel().getName(),
                    () -> deleteModel(model.getId())
                );
                dialog.open();
            }
        });
    }

    /**
     * Puts the thumbnail where the type icon would be. It carries an empty alt: the model's name is
     * right beside it as a heading, so describing the picture again would only make the row read twice.
     *
     * @param model the model whose description may hold a thumbnail
     */
    private void addThumbnail(QuickModelEntity model) {
        try {
            ModelService modelService = SpringContextUtils.getBean(ModelService.class);
            String thumbnailUrl = modelService.extractThumbnailDataUrl(model.getDescription());

            if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                Image thumbnail = new Image(thumbnailUrl, "");
                thumbnail.setWidth(THUMBNAIL_SIZE);
                thumbnail.setHeight(THUMBNAIL_SIZE);
                thumbnail.getStyle()
                        .set("object-fit", "cover")
                        .set("border-radius", "var(--lumo-border-radius-m)")
                        .set("flex", "0 0 auto");
                setLeadingVisual(thumbnail);
            }
        } catch (Exception ex) {
            log.warn("Failed to extract thumbnail from model description: {}", ex.getMessage());
        }
    }

    private void deleteModel(String modelId) {
        UI sourceUi = UI.getCurrent();
        runBackendCallWithOverlay(() -> {
                    ModelService modelService = SpringContextUtils.getBean(ModelService.class);
                    return modelService.delete(modelId);
                }, deleted -> {
            if (deleted) {
                if (isUiInActive(sourceUi)) {
                    return;
                }
                new SuccessNotification(text("model.delete.success"));
                refreshParentListingFromBackend();
            } else {
                if (isUiInActive(sourceUi)) {
                    return;
                }
                new ErrorNotification(text("model.delete.failed"));
            }
        }, ex -> {
            log.error("Error deleting model: {}", ex.getMessage(), ex);
            if (isUiInActive(sourceUi)) {
                return;
            }
            new ErrorNotification(text("model.delete.error") + ": " + ex.getMessage());
        });
    }

    private boolean isUiInActive(UI ui) {
        return ui == null || ui.getSession() == null || !ui.isAttached() || ui.isClosing();
    }
}
