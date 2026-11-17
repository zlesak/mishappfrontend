package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.component.html.Image;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.QuickChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterCreateView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterDetailView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelDetailView;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * List item component representing a chapter in listing and administration views.
 */
@Slf4j
public class ChapterListItem extends EntityRow {

    /** Big enough to recognise the preparation, small enough to keep the row one line tall. */
    private static final String THUMBNAIL_SIZE = "56px";

    /**
     * Constructs the chapter list item.
     *
     * @param chapter           the chapter entity to display
     * @param listView          whether to render in list (read) mode
     * @param administrationView whether to show edit and delete controls
     */
    public ChapterListItem(QuickChapterEntity chapter, boolean listView, boolean administrationView) {
        super(listView, administrationView, VaadinIcon.OPEN_BOOK);

        setRowTitle(chapter.getName());
        addCommonMetadata(chapter);
        addModelLinks(chapter);
        addMainModelThumbnail(chapter);

        setOpenButtonClickListener(e -> {
            VaadinSession.getCurrent().setAttribute("chapterEntity", chapter);
            UI.getCurrent().navigate(ChapterDetailView.class, new RouteParameters(new RouteParam("chapterId", chapter.getId())));
        });

        setEditButtonClickListener(e -> {
            if (administrationView) {
                VaadinSession.getCurrent().setAttribute("chapterEntity", chapter);
                UI.getCurrent().navigate(ChapterCreateView.class, new RouteParameters(new RouteParam("chapterId", chapter.getId())));
            }
        });

        setDeleteButtonClickListener(e -> {
            if (administrationView) {
                ConfirmDialog dialog = ConfirmDialog.createDeleteConfirmation(
                    "chapter",
                    chapter.getName(),
                    () -> deleteChapter(chapter.getId())
                );
                dialog.open();
            }
        });
    }

    /**
     * Lists the chapter's models as links.
     *
     * <p>They used to be clickable {@code Span}s, which no keyboard can reach and no screen reader
     * announces as anything to activate, laid out by a ResizeObserver that measured how many would fit
     * into a 240 px card and hid the rest behind a "+3" pill. A full-width row has room for the names,
     * so the measuring goes away and each model becomes an ordinary link.
     *
     * @param chapter the chapter whose models to list
     */
    private void addModelLinks(QuickChapterEntity chapter) {
        if (chapter.getModels() == null || chapter.getModels().isEmpty()) {
            return;
        }

        Map<String, QuickModelEntity> distinctModels = new LinkedHashMap<>();
        for (QuickModelEntity model : chapter.getModels()) {
            if (model == null || model.getModel() == null) {
                continue;
            }
            String name = model.getModel().getName();
            String routeModelId = model.getId() != null && !model.getId().isBlank()
                    ? model.getId()
                    : model.getModel().getId();
            if (name == null || name.isBlank() || routeModelId == null || routeModelId.isBlank()) {
                continue;
            }
            distinctModels.putIfAbsent(model.getModel().getId(), model);
        }

        if (distinctModels.isEmpty()) {
            return;
        }

        HorizontalLayout models = new HorizontalLayout();
        models.setPadding(false);
        models.setSpacing(false);
        models.setWrap(true);
        models.getStyle().set("gap", "var(--lumo-space-xs)").set("align-items", "baseline");

        Span label = new Span(text("chapter.models") + ": ");
        label.addClassName(LumoUtility.TextColor.SECONDARY);
        models.add(label);

        for (QuickModelEntity model : distinctModels.values()) {
            String routeModelId = model.getId() != null && !model.getId().isBlank()
                    ? model.getId()
                    : model.getModel().getId();
            RouterLink link = new RouterLink();
            link.setRoute(ModelDetailView.class, new RouteParameters(new RouteParam("modelId", routeModelId)));
            link.setText(model.getModel().getName());
            link.addClassNames("chapter-model-link", LumoUtility.FontSize.SMALL);
            models.add(link);
        }

        addMetadata(models);
    }

    /**
     * Shows the chapter's first model instead of the generic book icon.
     *
     * <p>A row of identical icons distinguishes nothing. The thumbnail is the picture the teacher
     * recognises the chapter by, and it is already stored with the model, so nothing extra is loaded.
     * Chapters whose model has no thumbnail keep the icon.
     *
     * @param chapter the chapter being listed
     */
    private void addMainModelThumbnail(QuickChapterEntity chapter) {
        if (chapter.getModels() == null || chapter.getModels().isEmpty()) {
            return;
        }
        try {
            ModelService modelService = SpringContextUtils.getBean(ModelService.class);
            for (QuickModelEntity model : chapter.getModels()) {
                if (model == null) {
                    continue;
                }
                String thumbnailUrl = modelService.extractThumbnailDataUrl(model.getDescription());
                if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
                    continue;
                }
                // Decorative: the chapter's name is the heading right beside it.
                Image thumbnail = new Image(thumbnailUrl, "");
                thumbnail.setWidth(THUMBNAIL_SIZE);
                thumbnail.setHeight(THUMBNAIL_SIZE);
                thumbnail.getStyle()
                        .set("object-fit", "cover")
                        .set("border-radius", "var(--lumo-border-radius-m)")
                        .set("flex", "0 0 auto");
                setLeadingVisual(thumbnail);
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to extract a chapter thumbnail: {}", ex.getMessage());
        }
    }

    private void deleteChapter(String chapterId) {
        UI sourceUi = UI.getCurrent();
        runBackendCallWithOverlay(() -> {
                    ChapterService chapterService = SpringContextUtils.getBean(ChapterService.class);
                    return chapterService.delete(chapterId);
                }, deleted -> {
            if (deleted) {
                if (isUiInActive(sourceUi)) {
                    return;
                }
                new SuccessNotification(text("chapter.delete.success"));
                refreshParentListingFromBackend();
            } else {
                if (isUiInActive(sourceUi)) {
                    return;
                }
                new ErrorNotification(text("chapter.delete.failed"));
            }
        }, ex -> {
            log.error("Error deleting chapter: {}", ex.getMessage(), ex);
            if (isUiInActive(sourceUi)) {
                return;
            }
            new ErrorNotification(text("chapter.delete.error") + ": " + ex.getMessage());
        });
    }

    private boolean isUiInActive(UI ui) {
        return ui == null || ui.getSession() == null || !ui.isAttached() || ui.isClosing();
    }
}
