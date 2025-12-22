package cz.uhk.zlesak.threejslearningapp.components.lists;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Tag("div")
public class ChapterListItem extends AbstractListItem {

    public ChapterListItem(ChapterEntity chapter, boolean listView) {
        super(listView);

        HorizontalLayout chapterName = new HorizontalLayout();
        Span nameLabel = new Span(text("chapter.title") + ": ");
        Span name = new Span(chapter.getName());
        name.getStyle().set("font-weight", "600");
        chapterName.add(nameLabel, name);
        details.add(chapterName);

        if (chapter.getCreatorId() != null && !chapter.getCreatorId().isBlank()) {
            HorizontalLayout creatorRow = new HorizontalLayout();
            creatorRow.add(new Span(text("chapter.creator") + ": "), new Span(chapter.getCreatorId()));
            details.add(creatorRow);
        }
        if (chapter.getCreated() != null) {
            HorizontalLayout dateRow = new HorizontalLayout();
            dateRow.add(new Span(text("chapter.creationDate") + ": "), new Span(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(chapter.getCreated())));
            details.add(dateRow);
        }
        if (chapter.getUpdated() != null) {
            HorizontalLayout dateRow = new HorizontalLayout();
            dateRow.add(new Span(text("chapter.lastModified") + ": "), new Span(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(chapter.getUpdated())));
            details.add(dateRow);
        }
        if (chapter.getModels() != null && !chapter.getModels().isEmpty()) {
            HorizontalLayout modelsRow = new HorizontalLayout();
            modelsRow.setAlignItems(HorizontalLayout.Alignment.CENTER);
            modelsRow.add(new Span(text("chapter.models") + ": "));
            for (QuickModelEntity model : chapter.getModels()) {
                if (model != null && model.getModel() != null) {
                    String modelName = model.getModel().getName();
                    String id = model.getModel().getId();
                    if (modelName != null && !modelName.isBlank() && id != null) {
                        Button modelButton = new Button(modelName);
                        modelButton.getStyle()
                                .set("background", "none")
                                .set("border", "none")
                                .set("color", "#1976d2")
                                .set("text-decoration", "underline")
                                .set("padding", "0")
                                .set("margin-right", "8px")
                                .set("cursor", "pointer");
                        modelButton.addClickListener(e -> {
                            VaadinSession.getCurrent().setAttribute("quickModelEntity", model);
                            UI.getCurrent().navigate("model/" + id);
                        });

                        modelsRow.add(modelButton);
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
