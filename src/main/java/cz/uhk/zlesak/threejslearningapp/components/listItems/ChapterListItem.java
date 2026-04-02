package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.ContextMenu;
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

import java.util.HashMap;

/**
 * List item component representing a chapter in listing and administration views.
 */
@Slf4j
@Tag("div")
public class ChapterListItem extends AbstractListItem {
    /**
     * Constructs the chapter list item.
     *
     * @param chapter           the chapter entity to display
     * @param listView          whether to render in list (read) mode
     * @param administrationView whether to show edit and delete controls
     */
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
            modelsRow.getStyle().set("min-width", "0");

            Icon cubeIcon = VaadinIcon.CUBE.create();
            cubeIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);
            cubeIcon.getElement().setProperty("title", text("chapter.models"));

            Span modelsLabel = new Span(text("chapter.models") + ":");
            modelsLabel.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.Display.HIDDEN,
                    LumoUtility.Display.Breakpoint.XLarge.INLINE
            );
            modelsLabel.setVisible(false);

            HorizontalLayout modelBadgesContainer = new HorizontalLayout();
            modelBadgesContainer.addClassNames(
                    LumoUtility.Gap.XSMALL,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.FlexWrap.NOWRAP
            );
            modelBadgesContainer.setSpacing(true);
            modelBadgesContainer.setPadding(false);
            modelBadgesContainer.setWidthFull();
            modelBadgesContainer.getStyle().set("min-width", "0");
            modelBadgesContainer.getStyle().set("overflow", "hidden");

            modelsRow.add(cubeIcon, modelsLabel, modelBadgesContainer);
            modelsRow.expand(modelBadgesContainer);

            HashMap<String, QuickModelEntity> addedModels = new HashMap<>();
            for (QuickModelEntity model : chapter.getModels()) {
                if (model != null && model.getModel() != null) {
                    if (addedModels.containsKey(model.getModel().getId())) {
                        continue;
                    }
                    addedModels.put(model.getModel().getId(), model);
                    String modelName = model.getModel().getName();
                    String routeModelId = model.getMetadataId() != null && !model.getMetadataId().isBlank()
                            ? model.getMetadataId()
                            : model.getModel().getId();
                    if (modelName == null || modelName.isBlank() || routeModelId == null || routeModelId.isBlank()) {
                        continue;
                    }

                    Span modelBadge = new Span(modelName);
                    modelBadge.addClassNames(
                            LumoUtility.Background.CONTRAST_10,
                            LumoUtility.TextColor.BODY,
                            LumoUtility.BorderRadius.SMALL,
                            LumoUtility.Padding.Horizontal.SMALL,
                            LumoUtility.Padding.Vertical.XSMALL,
                            LumoUtility.FontSize.SMALL
                    );
                    modelBadge.addClassName("chapter-model-badge");
                    modelBadge.getElement().setAttribute("data-model-name", modelName);
                    modelBadge.getStyle()
                            .set("cursor", "pointer")
                            .set("white-space", "nowrap")
                            .set("display", "inline-flex")
                            .set("justify-content", "flex-start");
                    modelBadge.getElement().setProperty("title", modelName);

                    modelBadge.getElement().addEventListener("click", e -> {
                        VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
                        UI.getCurrent().navigate(ModelDetailView.class, new RouteParameters(new RouteParam("modelId", routeModelId)));
                    });
                    modelBadgesContainer.add(modelBadge);
                }
            }

            Span overflowBadge = new Span("+0");
            overflowBadge.addClassNames(
                    LumoUtility.Background.CONTRAST_10,
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.BorderRadius.SMALL,
                    LumoUtility.Padding.Horizontal.SMALL,
                    LumoUtility.Padding.Vertical.XSMALL,
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.FontWeight.SEMIBOLD
            );
            overflowBadge.addClassName("chapter-model-overflow");
            overflowBadge.getStyle()
                    .set("display", "none")
                    .set("cursor", "pointer")
                    .set("white-space", "nowrap")
                    .set("justify-content", "center")
                    .set("text-align", "center")
                    .set("min-width", "40px");
            overflowBadge.getElement().setProperty("aria-label", "Další modely");
            modelBadgesContainer.add(overflowBadge);

            ContextMenu overflowMenu = new ContextMenu(overflowBadge);
            overflowMenu.setOpenOnClick(true);
            for (QuickModelEntity model : addedModels.values()) {
                if (model == null || model.getModel() == null) {
                    continue;
                }
                String modelName = model.getModel().getName();
                String routeModelId = model.getMetadataId() != null && !model.getMetadataId().isBlank()
                        ? model.getMetadataId()
                        : model.getModel().getId();
                if (modelName == null || modelName.isBlank() || routeModelId == null || routeModelId.isBlank()) {
                    continue;
                }
                overflowMenu.addItem(modelName, event -> {
                    VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
                    UI.getCurrent().navigate(ModelDetailView.class, new RouteParameters(new RouteParam("modelId", routeModelId)));
                });
            }

            modelBadgesContainer.getElement().executeJs("""
                const container = this;
                const overflow = container.querySelector('.chapter-model-overflow');
                const badges = () => Array.from(container.querySelectorAll('.chapter-model-badge'));

                const getGap = () => {
                  const style = getComputedStyle(container);
                  const raw = style.columnGap || style.gap || '8px';
                  const parsed = Number.parseFloat(raw);
                  return Number.isFinite(parsed) ? parsed : 8;
                };

                const relayout = () => {
                  if (!overflow) return;
                  const allBadges = badges();
                  allBadges.forEach((badge) => {
                    badge.style.display = 'inline-flex';
                    badge.style.flex = '0 0 auto';
                    badge.style.minWidth = '';
                    badge.style.maxWidth = '';
                    badge.style.overflow = 'hidden';
                    badge.style.textOverflow = 'ellipsis';
                    badge.style.whiteSpace = 'nowrap';
                  });
                  overflow.style.display = 'none';
                  overflow.style.flex = '0 0 auto';
                  overflow.textContent = '+0';
                  overflow.title = '';

                  const available = container.clientWidth;
                  if (available <= 0 || allBadges.length === 0) return;

                  if (allBadges.length === 1) {
                    const only = allBadges[0];
                    only.style.flex = '1 1 auto';
                    only.style.minWidth = '0';
                    only.style.maxWidth = '100%';
                    only.style.overflow = 'hidden';
                    only.style.textOverflow = 'ellipsis';
                    return;
                  }

                  const gap = getGap();
                  const widths = allBadges.map((badge) => badge.offsetWidth);
                  const measureOverflowWidth = (hiddenCount) => {
                    overflow.style.display = 'inline-flex';
                    overflow.style.visibility = 'hidden';
                    overflow.textContent = `+${hiddenCount}`;
                    return overflow.offsetWidth;
                  };

                  let visibleCount = 0;
                  let used = 0;
                  for (let i = 0; i < allBadges.length; i++) {
                    const remainingAfter = allBadges.length - (i + 1);
                    const reserveForOverflow = remainingAfter > 0
                      ? measureOverflowWidth(remainingAfter) + gap
                      : 0;
                    const nextUsed = (visibleCount > 0 ? used + gap : used) + widths[i];
                    if (nextUsed + reserveForOverflow <= available) {
                      used = nextUsed;
                      visibleCount++;
                    } else {
                      break;
                    }
                  }

                  if (visibleCount === 0) {
                    visibleCount = 1;
                  }

                  const hiddenCount = allBadges.length - visibleCount;
                  if (hiddenCount <= 0) {
                    overflow.style.display = 'none';
                    overflow.style.visibility = 'visible';
                    return;
                  }

                  allBadges.forEach((badge, index) => {
                    badge.style.display = index < visibleCount ? 'inline-flex' : 'none';
                  });

                  if (visibleCount === 1) {
                    const firstVisible = allBadges[0];
                    firstVisible.style.flex = '1 1 auto';
                    firstVisible.style.minWidth = '0';
                  }

                  overflow.style.display = 'inline-flex';
                  overflow.style.visibility = 'visible';
                  overflow.textContent = `+${hiddenCount}`;

                  const hiddenBadges = allBadges.slice(visibleCount);
                  const hiddenNames = hiddenBadges
                    .map((badge) => badge.getAttribute('data-model-name') || '')
                    .filter(Boolean);
                  overflow.title = hiddenNames.join(', ');
                };

                if (container.__chapterModelsResizeObserver) {
                  container.__chapterModelsResizeObserver.disconnect();
                }
                const observer = new ResizeObserver(() => relayout());
                observer.observe(container);
                container.__chapterModelsResizeObserver = observer;
                requestAnimationFrame(relayout);
                """);

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
