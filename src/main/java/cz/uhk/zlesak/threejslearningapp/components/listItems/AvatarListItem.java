package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * AvatarListItem class - A custom list item component that displays a user's avatar and name.
 */
public class AvatarListItem extends Div {

    /**
     * Constructs an AvatarListItem with the given name. The avatar is generated based on the name, and the full name is displayed next to it.
     * @param name the name of the user to display in the list item, which is also used to generate the avatar.
     */
    public AvatarListItem(String name) {
        HorizontalLayout userInfo = new HorizontalLayout();
        userInfo.addClassName("userMenuHeader");
        userInfo.setSpacing(false);

        Avatar userAvatar = new Avatar(name);
        userAvatar.getElement().setAttribute("tabindex", "-1");
        userAvatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
        userAvatar.addClassNames(LumoUtility.Margin.XSMALL);

        VerticalLayout nameLayout = new VerticalLayout();
        nameLayout.setSpacing(false);

        Div fullName = new Div(name);
        fullName.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SECONDARY);

        nameLayout.addClassName(LumoUtility.Padding.SMALL);

        nameLayout.add(fullName);

        userInfo.add(userAvatar, nameLayout);
        userInfo.setAlignItems(FlexComponent.Alignment.CENTER);
        add(userInfo);
    }
}
