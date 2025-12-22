package cz.uhk.zlesak.threejslearningapp.components.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class AvatarItem extends Composite<HorizontalLayout> implements HasSize {

    private final Span heading = new Span();

    private final Span description = new Span();

    public AvatarItem(String head, String desc, Avatar av) {
        getContent().setAlignItems(FlexComponent.Alignment.CENTER);

        description.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size",
                "var(--lumo-font-size-s)");

        VerticalLayout column = new VerticalLayout(heading, description);
        column.setPadding(false);
        column.setSpacing(false);

        getContent().add(column);
        getContent().getStyle().set("line-height", "var(--lumo-line-height-m)");

        setHeading(head);
        setDescription(desc);
        setAvatar(av);
    }

    public void setHeading(String text) {
        heading.setText(text);
    }

    public void setDescription(String text) {
        description.setText(text);
    }

    public void setAvatar(Avatar avatar) {
        if (getContent().getComponentAt(0) instanceof Avatar existing) {
            existing.removeFromParent();
        }
        getContent().addComponentAsFirst(avatar);
    }
}
