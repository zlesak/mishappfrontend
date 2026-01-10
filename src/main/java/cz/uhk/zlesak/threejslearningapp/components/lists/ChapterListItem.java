package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Tag("div")
public class ChapterListItem extends AbstractListItem {

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

            creatorRow.add(userIcon, label, value);
            details.add(creatorRow);
        }

        if (chapter.getCreated() != null) {
            HorizontalLayout dateRow = new HorizontalLayout();
            dateRow.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

            Icon calendarIcon = VaadinIcon.CALENDAR.create();
            calendarIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span label = new Span(text("chapter.creationDate") + ":");
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Span value = new Span(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(chapter.getCreated()));
            value.addClassNames(LumoUtility.FontSize.SMALL);

            dateRow.add(calendarIcon, label, value);
            details.add(dateRow);
        }

        if (chapter.getUpdated() != null) {
            HorizontalLayout updateRow = new HorizontalLayout();
            updateRow.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

            Icon editIcon = VaadinIcon.EDIT.create();
            editIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span label = new Span(text("chapter.lastModified") + ":");
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Span value = new Span(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(chapter.getUpdated()));
            value.addClassNames(LumoUtility.FontSize.SMALL);

            updateRow.add(editIcon, label, value);
            details.add(updateRow);
        }

        if (chapter.getModels() != null && !chapter.getModels().isEmpty()) {
            HorizontalLayout modelsRow = new HorizontalLayout();
            modelsRow.addClassNames(
                LumoUtility.Gap.SMALL,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.FlexWrap.WRAP
            );

            Icon cubeIcon = VaadinIcon.CUBE.create();
            cubeIcon.addClassNames(LumoUtility.IconSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span modelsLabel = new Span(text("chapter.models") + ":");
            modelsLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            modelsRow.add(cubeIcon, modelsLabel);

            for (QuickModelEntity model : chapter.getModels()) {
                if (model != null && model.getModel() != null) {
                    String modelName = model.getModel().getName();
                    String id = model.getModel().getId();
                    if (modelName != null && !modelName.isBlank() && id != null) {
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
                            UI.getCurrent().navigate("model/" + id);
                        });

                        modelsRow.add(modelBadge);
                    }
                }
            }

            details.add(modelsRow);
        }

        setOpenButtonClickListener(e -> {
            VaadinSession.getCurrent().setAttribute("chapterEntity", chapter);
            UI.getCurrent().navigate("chapter/" + chapter.getId());
        });
    }
}
