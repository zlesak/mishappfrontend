package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.DateFormater;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterCreateView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterDetailView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelDetailView;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag("div")
public class ChapterListItem extends AbstractListItem {
    private static final int MAX_VISIBLE_MODEL_BADGES = 2;

    public ChapterListItem(ChapterEntity chapter, boolean listView, boolean administrationView) {
        super(listView, administrationView, VaadinIcon.OPEN_BOOK);

        titleSpan.setText(chapter.getName());

        if (chapter.getCreatorId() != null && !chapter.getCreatorId().isBlank()) {
            HorizontalLayout creatorRow = new HorizontalLayout();
            creatorRow.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

            Icon userIcon = VaadinIcon.USER.create();
            userIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span label = new Span(text("chapter.creator") + ":");
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Span value = new Span(chapter.getCreatorId());
            value.addClassNames(LumoUtility.FontSize.SMALL);

            creatorRow.setWidthFull();

            label.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.Display.HIDDEN,
                    LumoUtility.Display.Breakpoint.XLarge.INLINE
            );

            value.addClassNames(LumoUtility.FontSize.SMALL);

            applyEllipsis(value);
            value.getElement().setProperty("title", value.getText());

            creatorRow.add(userIcon, label, value);
            creatorRow.expand(value);
            details.add(creatorRow);
        }

        if (chapter.getCreated() != null) {
            HorizontalLayout dateRow = new HorizontalLayout();

            dateRow.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

            Icon calendarIcon = VaadinIcon.CALENDAR.create();
            calendarIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span label = new Span(text("chapter.creationDate") + ":");
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Span value = new Span(DateFormater.formatDate(chapter.getCreated()));
            value.addClassNames(LumoUtility.FontSize.SMALL);

            dateRow.setWidthFull();

            label.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.Display.HIDDEN,
                    LumoUtility.Display.Breakpoint.XLarge.INLINE
            );

            value.addClassNames(LumoUtility.FontSize.SMALL);

            applyEllipsis(value);
            value.getElement().setProperty("title", value.getText());

            dateRow.add(calendarIcon, label, value);
            dateRow.expand(value);
            details.add(dateRow);
        }

        if (chapter.getUpdated() != null) {
            HorizontalLayout updateRow = new HorizontalLayout();
            updateRow.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

            Icon editIcon = VaadinIcon.EDIT.create();
            editIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span label = new Span(text("chapter.lastModified") + ":");
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Span value = new Span(DateFormater.formatDate(chapter.getUpdated()));
            value.addClassNames(LumoUtility.FontSize.SMALL);
            updateRow.setWidthFull();

            label.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.Display.HIDDEN,
                    LumoUtility.Display.Breakpoint.XLarge.INLINE
            );

            value.addClassNames(LumoUtility.FontSize.SMALL);

            applyEllipsis(value);
            value.getElement().setProperty("title", value.getText());

            updateRow.add(editIcon, label, value);
            updateRow.expand(value);
            details.add(updateRow);
        }

        if (chapter.getModels() != null && !chapter.getModels().isEmpty()) {
            HorizontalLayout modelsRow = new HorizontalLayout();
            modelsRow.addClassNames(
                LumoUtility.Gap.SMALL,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.FlexWrap.NOWRAP
            );
            modelsRow.setWidthFull();
            modelsRow.getStyle().set("overflow-x", "auto");
            modelsRow.getStyle().set("overflow-y", "hidden");

            Icon cubeIcon = VaadinIcon.CUBE.create();
            cubeIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span modelsLabel = new Span(text("chapter.models") + ":");
            modelsLabel.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.Display.HIDDEN,
                    LumoUtility.Display.Breakpoint.XLarge.INLINE
            );

            modelsRow.add(cubeIcon, modelsLabel);

            HashMap<String, QuickModelEntity> addedModels = new HashMap<>();
            List<QuickModelEntity> uniqueModels = new ArrayList<>();
            for (QuickModelEntity model : chapter.getModels()) {
                if (model != null && model.getModel() != null) {
                    if (addedModels.containsKey(model.getModel().getId())) {
                        continue;
                    }
                    addedModels.put(model.getModel().getId(), model);
                    uniqueModels.add(model);
                }
            }

            int visibleCount = Math.min(MAX_VISIBLE_MODEL_BADGES, uniqueModels.size());
            for (int i = 0; i < visibleCount; i++) {
                QuickModelEntity model = uniqueModels.get(i);
                String modelName = model.getModel().getName();
                String routeModelId = model.getMetadataId() != null && !model.getMetadataId().isBlank()
                        ? model.getMetadataId()
                        : model.getModel().getId();
                if (modelName != null && !modelName.isBlank() && routeModelId != null) {
                    Span modelBadge = new Span(modelName);
                    modelBadge.addClassNames(
                        LumoUtility.Background.CONTRAST_10,
                        LumoUtility.TextColor.BODY,
                        LumoUtility.BorderRadius.SMALL,
                        LumoUtility.Padding.Horizontal.SMALL,
                        LumoUtility.Padding.Vertical.XSMALL,
                        LumoUtility.FontSize.SMALL
                    );
                    modelBadge.getElement().getStyle().set("cursor", "pointer");

                    modelBadge.getElement().addEventListener("click", e -> {
                        VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
                        UI.getCurrent().navigate(ModelDetailView.class, new RouteParameters(new RouteParam("modelId", routeModelId)));
                    });

                    modelBadge.getStyle()
                            .set("max-width", "120px")
                            .set("white-space", "nowrap")
                            .set("overflow", "hidden")
                            .set("text-overflow", "ellipsis");

                    modelBadge.getElement().setProperty("title", modelName);
                    modelsRow.add(modelBadge);
                }
            }

            int hiddenCount = uniqueModels.size() - visibleCount;
            if (hiddenCount > 0) {
                Span moreBadge = new Span("+" + hiddenCount);
                moreBadge.addClassNames(
                        LumoUtility.Background.CONTRAST_10,
                        LumoUtility.TextColor.SECONDARY,
                        LumoUtility.BorderRadius.SMALL,
                        LumoUtility.Padding.Horizontal.SMALL,
                        LumoUtility.Padding.Vertical.XSMALL,
                        LumoUtility.FontSize.SMALL,
                        LumoUtility.FontWeight.SEMIBOLD
                );
                String hiddenModels = uniqueModels.stream()
                        .skip(visibleCount)
                        .map(quickModelEntity -> quickModelEntity.getModel() != null ? quickModelEntity.getModel().getName() : null)
                        .filter(name -> name != null && !name.isBlank())
                        .collect(Collectors.joining(", "));
                if (!hiddenModels.isBlank()) {
                    moreBadge.getElement().setProperty("title", hiddenModels);
                }
                modelsRow.add(moreBadge);
            }

            details.add(modelsRow);
        }

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
