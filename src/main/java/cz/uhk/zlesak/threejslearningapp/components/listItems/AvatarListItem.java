package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LogoutButton;

public class AvatarListItem extends Div {

    public AvatarListItem(String name, String description, LogoutButton logoutButton) {
        HorizontalLayout userInfo = new HorizontalLayout();
        userInfo.addClassName("userMenuHeader");
        userInfo.setSpacing(false);

        Avatar userAvatar = new Avatar(name);
        userAvatar.getElement().setAttribute("tabindex", "-1");
        userAvatar.addThemeVariants(AvatarVariant.LUMO_SMALL);

        VerticalLayout nameLayout = new VerticalLayout();
        nameLayout.setSpacing(false);
        nameLayout.setPadding(false);

        Div fullName = new Div(name);
        fullName.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD);

        Div descriptionDiv = new Div(description);
        descriptionDiv.addClassNames(LumoUtility.FontSize.SMALL);

        nameLayout.addClassName(LumoUtility.Padding.SMALL);

        nameLayout.add(fullName, descriptionDiv);

        userInfo.add(userAvatar, nameLayout, logoutButton);
        userInfo.setAlignItems(FlexComponent.Alignment.CENTER);
        add(userInfo);
    }
}
