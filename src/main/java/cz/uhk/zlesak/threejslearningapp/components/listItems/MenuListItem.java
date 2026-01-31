package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MenuListItem extends ListItem {

    private final Class<? extends Component> view;

    public MenuListItem(String menuTitle, Component icon, Class<? extends Component> view) {
        this.view = view;
        RouterLink link = new RouterLink();
        link.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.XSMALL, LumoUtility.Height.MEDIUM, LumoUtility.AlignItems.CENTER, LumoUtility.Padding.Horizontal.SMALL, LumoUtility.TextColor.BODY);
        link.setRoute(view);
        Span text = new Span(menuTitle);
        text.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.MEDIUM, LumoUtility.Whitespace.NOWRAP);
        if (icon != null) {
            link.add(icon);
        }
        link.add(text);
        add(link);
    }

    public Class<?> getView() {
        return view;
    }
}
